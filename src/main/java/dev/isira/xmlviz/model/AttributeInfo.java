package dev.isira.xmlviz.model;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class AttributeInfo {
    @Getter
    private final String name;
    private final Set<String> sampleValues = new LinkedHashSet<>();
    private String inferredType;

    public AttributeInfo(String name) {
        this.name = name;
    }

    public String getInferredType() {
        return inferredType != null ? inferredType : "string";
    }

    public Set<String> getSampleValues() {
        return Collections.unmodifiableSet(sampleValues);
    }

    public void recordValue(String value) {
        if (sampleValues.size() < 10) {
            sampleValues.add(value);
        }
        inferType(value);
    }

    private void inferType(String value) {
        if (value == null || value.isEmpty()) return;
        if ("string".equals(inferredType)) return;

        final var detected = detectType(value);
        if (inferredType == null) {
            inferredType = detected;
        } else if (!inferredType.equals(detected)) {
            if (inferredType.equals("integer") && detected.equals("decimal")) {
                inferredType = "decimal";
            } else {
                inferredType = "string";
            }
        }
    }

    private String detectType(String v) {
        if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false")) return "boolean";
        try {
            Long.parseLong(v);
            return "integer";
        } catch (NumberFormatException ignored) {
        }
        try {
            Double.parseDouble(v);
            return "decimal";
        } catch (NumberFormatException ignored) {
        }
        if (v.matches("\\d{4}-\\d{2}-\\d{2}.*")) return "date";
        return "string";
    }
}
