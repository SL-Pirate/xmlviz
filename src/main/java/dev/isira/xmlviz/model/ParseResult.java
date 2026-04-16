package dev.isira.xmlviz.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ParseResult {
    @Getter
    private final Map<String, SchemaNode> schemaMap;
    @Getter
    private final List<IndexEntry> instanceIndex;
    private final Map<Integer, List<Integer>> childrenByParentId;
    @Getter
    private final long totalElements;

    public ParseResult(Map<String, SchemaNode> schemaMap,
                       List<IndexEntry> instanceIndex,
                       Map<Integer, List<Integer>> childrenByParentId,
                       long totalElements) {
        this.schemaMap = schemaMap;
        this.instanceIndex = instanceIndex;
        this.childrenByParentId = childrenByParentId;
        this.totalElements = totalElements;
    }

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
