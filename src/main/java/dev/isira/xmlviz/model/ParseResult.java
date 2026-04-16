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

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
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

    public String toXml(IndexEntry entry) {
        final int[] budget = {50_000};
        return toXml(entry, 0, budget);
    }

    private String toXml(IndexEntry entry, int indent, int[] budget) {
        if (budget[0]-- <= 0) {
            return "  ".repeat(indent) + "<!-- ... remaining elements omitted (limit reached) -->";
        }

        final var sb = new StringBuilder();
        sb.repeat("  ", indent);
        sb.append("<").append(entry.getTagName());

        for (final var attr : entry.getAttributes().entrySet()) {
            sb.append(" ").append(attr.getKey()).append("=\"").append(escapeXml(attr.getValue())).append("\"");
        }

        final var childIds = getChildIds(entry.getId());
        final var text = entry.getTextPreview();
        final var hasChildren = !childIds.isEmpty();
        final var hasText = text != null && !text.isEmpty();

        if (!hasChildren && !hasText) {
            sb.append("/>");
        } else {
            sb.append(">");
            if (hasText) {
                if (hasChildren) {
                    sb.append("\n").repeat("  ", indent + 1);
                }
                sb.append(escapeXml(text));
                if (entry.isHasMoreText()) {
                    sb.append("[truncated]");
                }
            }
            if (hasChildren) {
                sb.append("\n");
                for (final var childId : childIds) {
                    if (budget[0] <= 0) {
                        final var remaining = childIds.size() - childIds.indexOf(childId);
                        sb.repeat("  ", indent + 1)
                                .append("<!-- ... ").append(remaining).append(" more child elements omitted -->\n");
                        break;
                    }
                    final var child = instanceIndex.get(childId);
                    sb.append(toXml(child, indent + 1, budget)).append("\n");
                }
                sb.repeat("  ", indent);
            }
            sb.append("</").append(entry.getTagName()).append(">");
        }
        return sb.toString();
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
