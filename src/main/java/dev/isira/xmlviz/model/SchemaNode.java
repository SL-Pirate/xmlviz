package dev.isira.xmlviz.model;

import java.util.*;

public class SchemaNode {
    private final String tagName;
    private final Map<String, AttributeInfo> attributes = new LinkedHashMap<>();
    private final Map<String, ChildInfo> children = new LinkedHashMap<>();
    private int instanceCount;
    private boolean hasTextContent;

    public SchemaNode(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }

    public Map<String, AttributeInfo> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public Map<String, ChildInfo> getChildren() {
        return Collections.unmodifiableMap(children);
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public boolean isHasTextContent() {
        return hasTextContent;
    }

    public void incrementInstanceCount() {
        instanceCount++;
    }

    public void setHasTextContent(boolean hasTextContent) {
        this.hasTextContent = hasTextContent;
    }

    public void addAttribute(String name, String value) {
        attributes.computeIfAbsent(name, AttributeInfo::new).recordValue(value);
    }

    public Set<String> getKnownChildTypes() {
        return children.keySet();
    }

    public void updateChildOccurrence(String childTag, int count) {
        children.computeIfAbsent(childTag, k -> new ChildInfo()).recordOccurrence(count);
    }
}
