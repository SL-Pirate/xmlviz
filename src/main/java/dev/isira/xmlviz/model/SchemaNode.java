package dev.isira.xmlviz.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SchemaNode {
    @Getter
    private final String tagName;
    private final Map<String, AttributeInfo> attributes = new LinkedHashMap<>();
    private final Map<String, ChildInfo> children = new LinkedHashMap<>();
    @Getter
    private int instanceCount;
    @Setter
    @Getter
    private boolean hasTextContent;

    public SchemaNode(String tagName) {
        this.tagName = tagName;
    }

    public Map<String, AttributeInfo> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public Map<String, ChildInfo> getChildren() {
        return Collections.unmodifiableMap(children);
    }

    public void incrementInstanceCount() {
        instanceCount++;
    }

    public void addAttribute(String name, String value) {
        attributes.computeIfAbsent(name, AttributeInfo::new).recordValue(value);
    }

    public Set<String> getKnownChildTypes() {
        return children.keySet();
    }

    public void updateChildOccurrence(String childTag, int count) {
        children.computeIfAbsent(childTag, _ -> new ChildInfo()).recordOccurrence(count);
    }
}
