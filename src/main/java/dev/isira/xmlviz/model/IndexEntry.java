package dev.isira.xmlviz.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class IndexEntry {
    private final int id;
    private final String tagName;
    private final int depth;
    private final int parentId;
    private final Map<String, String> attributes;
    private int childCount;
    private String textPreview;
    private boolean hasMoreText;

    public IndexEntry(int id, String tagName, int depth, int parentId, Map<String, String> attributes) {
        this.id = id;
        this.tagName = tagName;
        this.depth = depth;
        this.parentId = parentId;
        this.attributes = attributes != null ? new LinkedHashMap<>(attributes) : Collections.emptyMap();
    }

    public int getId() { return id; }
    public String getTagName() { return tagName; }
    public int getDepth() { return depth; }
    public int getParentId() { return parentId; }
    public Map<String, String> getAttributes() { return attributes; }

    public int getChildCount() { return childCount; }
    public void setChildCount(int childCount) { this.childCount = childCount; }

    public String getTextPreview() { return textPreview; }
    public void setTextPreview(String textPreview) { this.textPreview = textPreview; }

    public boolean isHasMoreText() { return hasMoreText; }
    public void setHasMoreText(boolean hasMoreText) { this.hasMoreText = hasMoreText; }

    @Override
    public String toString() {
        final var sb = new StringBuilder("<").append(tagName);
        int shown = 0;
        for (var entry : attributes.entrySet()) {
            if (shown >= 3) { sb.append(" ..."); break; }
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

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
