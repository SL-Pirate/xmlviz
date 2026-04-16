package dev.isira.xmlviz.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class IndexEntry {
    private final int id;
    private final String tagName;
    private final int depth;
    private final int parentId;
    private final Map<String, String> attributes;
    @Setter
    private int childCount;
    @Setter
    private String textPreview;
    @Setter
    private boolean hasMoreText;

    public IndexEntry(int id, String tagName, int depth, int parentId, Map<String, String> attributes) {
        this.id = id;
        this.tagName = tagName;
        this.depth = depth;
        this.parentId = parentId;
        this.attributes = attributes != null ? new LinkedHashMap<>(attributes) : Collections.emptyMap();
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }

    public boolean matches(String lowerCaseQuery) {
        if (tagName.toLowerCase().contains(lowerCaseQuery)) return true;
        for (final var value : attributes.values()) {
            if (value.toLowerCase().contains(lowerCaseQuery)) return true;
        }
        return textPreview != null && textPreview.toLowerCase().contains(lowerCaseQuery);
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder("<").append(tagName);
        int shown = 0;
        for (var entry : attributes.entrySet()) {
            if (shown >= 3) {
                sb.append(" ...");
                break;
            }
            sb.append(" ").append(entry.getKey()).append("=\"").append(truncate(entry.getValue(), 20)).append("\"");
            shown++;
        }
        sb.append(">");
        if (childCount > 0) {
            sb.append(" (").append(childCount).append(" children)");
        }
        if (textPreview != null && !textPreview.isEmpty()) {
            sb.append(" \"").append(truncate(textPreview, 40)).append("\"");
        }
        return sb.toString();
    }
}
