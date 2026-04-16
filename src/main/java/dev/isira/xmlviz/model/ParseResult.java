package dev.isira.xmlviz.model;

import java.util.*;

/**
 * Holds the complete result of parsing an XML file:
 * the inferred schema map and the instance index for tree navigation.
 */
public class ParseResult {
    private final Map<String, SchemaNode> schemaMap;
    private final List<IndexEntry> instanceIndex;
    private final Map<Integer, List<Integer>> childrenByParentId;
    private final long totalElements;
    private final long fileSizeBytes;

    public ParseResult(Map<String, SchemaNode> schemaMap,
                       List<IndexEntry> instanceIndex,
                       Map<Integer, List<Integer>> childrenByParentId,
                       long totalElements,
                       long fileSizeBytes) {
        this.schemaMap = schemaMap;
        this.instanceIndex = instanceIndex;
        this.childrenByParentId = childrenByParentId;
        this.totalElements = totalElements;
        this.fileSizeBytes = fileSizeBytes;
    }

    public Map<String, SchemaNode> getSchemaMap() { return schemaMap; }
    public List<IndexEntry> getInstanceIndex() { return instanceIndex; }
    public Map<Integer, List<Integer>> getChildrenByParentId() { return childrenByParentId; }
    public long getTotalElements() { return totalElements; }
    public long getFileSizeBytes() { return fileSizeBytes; }

    /** Get direct children IDs of a given entry. */
    public List<Integer> getChildIds(int parentId) {
        return childrenByParentId.getOrDefault(parentId, Collections.emptyList());
    }

    /** Get root-level entries (parentId == -1). */
    public List<IndexEntry> getRootEntries() {
        List<Integer> rootIds = getChildIds(-1);
        List<IndexEntry> roots = new ArrayList<>(rootIds.size());
        for (int id : rootIds) {
            roots.add(instanceIndex.get(id));
        }
        return roots;
    }

    /** Build XPath for a given index entry by walking up the parent chain. */
    public String buildXPath(IndexEntry entry) {
        List<String> parts = new ArrayList<>();
        int currentId = entry.getId();
        while (currentId >= 0) {
            IndexEntry current = instanceIndex.get(currentId);
            int position = 1;
            List<Integer> siblings = getChildIds(current.getParentId());
            for (int sibId : siblings) {
                if (sibId == currentId) break;
                if (instanceIndex.get(sibId).getTagName().equals(current.getTagName())) {
                    position++;
                }
            }
            parts.add(current.getTagName() + "[" + position + "]");
            currentId = current.getParentId();
        }
        Collections.reverse(parts);
        return "/" + String.join("/", parts);
    }
}
