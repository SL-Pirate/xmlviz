package dev.isira.xmlviz.model;

import java.util.*;

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

    public List<Integer> getChildIds(int parentId) {
        return childrenByParentId.getOrDefault(parentId, Collections.emptyList());
    }

    public List<IndexEntry> getRootEntries() {
        final var rootIds = getChildIds(-1);
        final List<IndexEntry> roots = new ArrayList<>(rootIds.size());
        for (final var id : rootIds) {
            roots.add(instanceIndex.get(id));
        }
        return roots;
    }

    public String buildXPath(IndexEntry entry) {
        final List<String> parts = new ArrayList<>();
        int currentId = entry.getId();
        while (currentId >= 0) {
            final var current = instanceIndex.get(currentId);
            int position = 1;
            final var siblings = getChildIds(current.getParentId());
            for (final var sibId : siblings) {
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
