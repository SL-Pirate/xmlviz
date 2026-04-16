package dev.isira.xmlviz.parsing;

import dev.isira.xmlviz.model.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.function.Consumer;

/**
 * Single-pass StAX parser that builds both the SchemaMap (for ERD view)
 * and the InstanceIndex (for tree view) simultaneously.
 *
 * Designed to run on a background thread. Reports progress via callback.
 */
public class XmlParser {

    private static final int TEXT_PREVIEW_LENGTH = 200;
    private static final int PROGRESS_REPORT_INTERVAL = 10_000;

    public ParseResult parse(File file, Consumer<Double> progressCallback) throws Exception {
        long fileSize = file.length();

        Map<String, SchemaNode> schemaMap = new LinkedHashMap<>();
        List<IndexEntry> instanceIndex = new ArrayList<>();
        Map<Integer, List<Integer>> childrenByParentId = new HashMap<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        try (CountingInputStream cis = new CountingInputStream(
                new BufferedInputStream(new FileInputStream(file), 65536))) {

            XMLStreamReader reader = factory.createXMLStreamReader(cis, detectEncoding(file));

            Deque<ElementContext> stack = new ArrayDeque<>();

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT -> handleStartElement(
                            reader, stack, schemaMap, instanceIndex, childrenByParentId,
                            cis, fileSize, progressCallback);
                    case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA ->
                            handleCharacters(reader, stack, schemaMap);
                    case XMLStreamConstants.END_ELEMENT ->
                            handleEndElement(stack, schemaMap, instanceIndex, childrenByParentId);
                }
            }

            reader.close();
        }

        return new ParseResult(schemaMap, instanceIndex, childrenByParentId,
                instanceIndex.size(), fileSize);
    }

    private void handleStartElement(XMLStreamReader reader, Deque<ElementContext> stack,
                                    Map<String, SchemaNode> schemaMap,
                                    List<IndexEntry> instanceIndex,
                                    Map<Integer, List<Integer>> childrenByParentId,
                                    CountingInputStream cis, long fileSize,
                                    Consumer<Double> progressCallback) {
        String tag = reader.getLocalName();
        int depth = stack.size();
        int parentId = stack.isEmpty() ? -1 : stack.peek().indexPosition;

        // --- Schema tracking ---
        SchemaNode schemaNode = schemaMap.computeIfAbsent(tag, SchemaNode::new);
        schemaNode.incrementInstanceCount();

        int attrCount = reader.getAttributeCount();
        Map<String, String> attrs = attrCount > 0 ? new LinkedHashMap<>(attrCount) : null;
        for (int i = 0; i < attrCount; i++) {
            String attrName = reader.getAttributeLocalName(i);
            String attrValue = reader.getAttributeValue(i);
            schemaNode.addAttribute(attrName, attrValue);
            if (attrs == null) attrs = new LinkedHashMap<>();
            attrs.put(attrName, attrValue);
        }

        // Track child occurrence in parent context
        if (!stack.isEmpty()) {
            stack.peek().childCounts.merge(tag, 1, Integer::sum);
        }

        // --- Instance index ---
        int indexPos = instanceIndex.size();
        IndexEntry entry = new IndexEntry(indexPos, tag, depth, parentId, attrs);
        instanceIndex.add(entry);
        childrenByParentId.computeIfAbsent(parentId, k -> new ArrayList<>()).add(indexPos);

        // Push context
        stack.push(new ElementContext(tag, indexPos));

        // Progress reporting
        if (progressCallback != null && indexPos % PROGRESS_REPORT_INTERVAL == 0 && fileSize > 0) {
            double progress = Math.min((double) cis.getBytesRead() / fileSize, 1.0);
            progressCallback.accept(progress);
        }
    }

    private void handleCharacters(XMLStreamReader reader, Deque<ElementContext> stack,
                                  Map<String, SchemaNode> schemaMap) {
        if (stack.isEmpty()) return;
        if (reader.isWhiteSpace()) return;

        ElementContext ctx = stack.peek();
        schemaMap.get(ctx.tagName).setHasTextContent(true);

        // Accumulate text preview (cap the buffer to avoid memory waste)
        if (ctx.textBuffer.length() < TEXT_PREVIEW_LENGTH + 100) {
            ctx.textBuffer.append(reader.getText());
        }
    }

    private void handleEndElement(Deque<ElementContext> stack,
                                  Map<String, SchemaNode> schemaMap,
                                  List<IndexEntry> instanceIndex,
                                  Map<Integer, List<Integer>> childrenByParentId) {
        ElementContext ctx = stack.pop();
        SchemaNode schemaNode = schemaMap.get(ctx.tagName);

        // --- Finalize cardinality stats ---
        // Update counts for child types that appeared in this instance
        for (var childEntry : ctx.childCounts.entrySet()) {
            schemaNode.updateChildOccurrence(childEntry.getKey(), childEntry.getValue());
        }
        // Child types known from other instances but absent here → 0 occurrences
        for (String knownChild : new ArrayList<>(schemaNode.getKnownChildTypes())) {
            if (!ctx.childCounts.containsKey(knownChild)) {
                schemaNode.updateChildOccurrence(knownChild, 0);
            }
        }

        // --- Finalize IndexEntry ---
        IndexEntry entry = instanceIndex.get(ctx.indexPosition);
        List<Integer> children = childrenByParentId.get(ctx.indexPosition);
        entry.setChildCount(children != null ? children.size() : 0);

        String text = ctx.textBuffer.toString().trim();
        if (!text.isEmpty()) {
            if (text.length() > TEXT_PREVIEW_LENGTH) {
                entry.setTextPreview(text.substring(0, TEXT_PREVIEW_LENGTH));
                entry.setHasMoreText(true);
            } else {
                entry.setTextPreview(text);
            }
        }
    }

    /**
     * Best-effort encoding detection from the XML declaration.
     * Falls back to UTF-8.
     */
    private String detectEncoding(File file) {
        try (var fis = new FileInputStream(file)) {
            byte[] header = new byte[128];
            int read = fis.read(header);
            if (read > 0) {
                String headerStr = new String(header, 0, read, java.nio.charset.StandardCharsets.ISO_8859_1);
                int idx = headerStr.indexOf("encoding=");
                if (idx >= 0) {
                    int start = headerStr.indexOf('"', idx);
                    int end = headerStr.indexOf('"', start + 1);
                    if (start >= 0 && end > start) {
                        return headerStr.substring(start + 1, end);
                    }
                    start = headerStr.indexOf('\'', idx);
                    end = headerStr.indexOf('\'', start + 1);
                    if (start >= 0 && end > start) {
                        return headerStr.substring(start + 1, end);
                    }
                }
            }
        } catch (Exception ignored) {}
        return "UTF-8";
    }

    /** Per-element parsing context held on the stack. */
    private static class ElementContext {
        final String tagName;
        final int indexPosition;
        final Map<String, Integer> childCounts = new HashMap<>();
        final StringBuilder textBuffer = new StringBuilder();

        ElementContext(String tagName, int indexPosition) {
            this.tagName = tagName;
            this.indexPosition = indexPosition;
        }
    }
}
