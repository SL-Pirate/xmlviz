package dev.isira.xmlviz.parsing;

import dev.isira.xmlviz.model.IndexEntry;
import dev.isira.xmlviz.model.ParseResult;
import dev.isira.xmlviz.model.SchemaNode;
import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class XmlParser {

    private static final int TEXT_PREVIEW_LENGTH = 200;
    private static final int PROGRESS_REPORT_INTERVAL = 10_000;

    public ParseResult parse(File file, Consumer<Double> progressCallback) throws Exception {
        final var fileSize = file.length();

        final Map<String, SchemaNode> schemaMap = new LinkedHashMap<>();
        final List<IndexEntry> instanceIndex = new ArrayList<>();
        final Map<Integer, List<Integer>> childrenByParentId = new HashMap<>();

        final var factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        try (final var cis = new CountingInputStream(
                new BufferedInputStream(new FileInputStream(file), 65536))) {

            final var reader = factory.createXMLStreamReader(cis, detectEncoding(file));
            final Deque<ElementContext> stack = new ArrayDeque<>();

            while (reader.hasNext()) {
                final var event = reader.next();

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
                instanceIndex.size());
    }

    private void handleStartElement(XMLStreamReader reader, Deque<ElementContext> stack,
                                    Map<String, SchemaNode> schemaMap,
                                    List<IndexEntry> instanceIndex,
                                    Map<Integer, List<Integer>> childrenByParentId,
                                    CountingInputStream cis, long fileSize,
                                    Consumer<Double> progressCallback) {
        final var tag = reader.getLocalName();
        final var depth = stack.size();
        final var parentId = stack.isEmpty() ? -1 : stack.peek().indexPosition;

        // Schema tracking
        final var schemaNode = schemaMap.computeIfAbsent(tag, SchemaNode::new);
        schemaNode.incrementInstanceCount();

        final var attrCount = reader.getAttributeCount();
        final Map<String, String> attrs = attrCount > 0 ? new LinkedHashMap<>(attrCount) : null;
        for (int i = 0; i < attrCount; i++) {
            final var attrName = reader.getAttributeLocalName(i);
            final var attrValue = reader.getAttributeValue(i);
            schemaNode.addAttribute(attrName, attrValue);
            attrs.put(attrName, attrValue);
        }

        if (!stack.isEmpty()) {
            stack.peek().childCounts.merge(tag, 1, Integer::sum);
        }

        // Instance index
        final var indexPos = instanceIndex.size();
        final var entry = new IndexEntry(indexPos, tag, depth, parentId, attrs);
        instanceIndex.add(entry);
        childrenByParentId.computeIfAbsent(parentId, _ -> new ArrayList<>()).add(indexPos);

        stack.push(new ElementContext(tag, indexPos));

        if (progressCallback != null && indexPos % PROGRESS_REPORT_INTERVAL == 0 && fileSize > 0) {
            final var progress = Math.min((double) cis.getBytesRead() / fileSize, 1.0);
            progressCallback.accept(progress);
        }
    }

    private void handleCharacters(XMLStreamReader reader, Deque<ElementContext> stack,
                                  Map<String, SchemaNode> schemaMap) {
        if (stack.isEmpty()) return;
        if (reader.isWhiteSpace()) return;

        final var ctx = stack.peek();
        if (ctx == null) {
            log.error("CTX is null! Bailing out");
            return;
        }
        schemaMap.get(ctx.tagName).setHasTextContent(true);

        if (ctx.textBuffer.length() < TEXT_PREVIEW_LENGTH + 100) {
            ctx.textBuffer.append(reader.getText());
        }
    }

    private void handleEndElement(Deque<ElementContext> stack,
                                  Map<String, SchemaNode> schemaMap,
                                  List<IndexEntry> instanceIndex,
                                  Map<Integer, List<Integer>> childrenByParentId) {
        final var ctx = stack.pop();
        final var schemaNode = schemaMap.get(ctx.tagName);

        for (var childEntry : ctx.childCounts.entrySet()) {
            schemaNode.updateChildOccurrence(childEntry.getKey(), childEntry.getValue());
        }
        for (final var knownChild : new ArrayList<>(schemaNode.getKnownChildTypes())) {
            if (!ctx.childCounts.containsKey(knownChild)) {
                schemaNode.updateChildOccurrence(knownChild, 0);
            }
        }

        final var entry = instanceIndex.get(ctx.indexPosition);
        final var children = childrenByParentId.get(ctx.indexPosition);
        entry.setChildCount(children != null ? children.size() : 0);

        final var text = ctx.textBuffer.toString().trim();
        if (!text.isEmpty()) {
            if (text.length() > TEXT_PREVIEW_LENGTH) {
                entry.setTextPreview(text.substring(0, TEXT_PREVIEW_LENGTH));
                entry.setHasMoreText(true);
            } else {
                entry.setTextPreview(text);
            }
        }
    }

    String detectEncoding(File file) {
        try (final var fis = new FileInputStream(file)) {
            final var header = new byte[128];
            final var read = fis.read(header);
            if (read > 0) {
                final var headerStr = new String(header, 0, read, java.nio.charset.StandardCharsets.ISO_8859_1);
                final var idx = headerStr.indexOf("encoding=");
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
        } catch (Exception ignored) {
        }
        return "UTF-8";
    }

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
