package dev.isira.xmlviz.mcp;

import dev.isira.xmlviz.model.SchemaNode;

import java.util.Map;
import java.util.StringJoiner;

public final class SchemaFormatter {

    private SchemaFormatter() {
    }

    public static String formatAll(Map<String, SchemaNode> schemaMap) {
        final var sb = new StringBuilder();
        var first = true;
        for (final var node : schemaMap.values()) {
            if (!first) sb.append("\n\n");
            sb.append(formatNode(node));
            first = false;
        }
        return sb.toString();
    }

    public static String formatNode(SchemaNode node) {
        final var sb = new StringBuilder();
        sb.append(node.getTagName());
        sb.append(" (").append(node.getInstanceCount());
        sb.append(node.getInstanceCount() == 1 ? " instance)" : " instances)");

        final var children = node.getChildren();
        if (!children.isEmpty()) {
            sb.append("\n  children: ");
            final var joiner = new StringJoiner(", ");
            for (final var child : children.entrySet()) {
                joiner.add(child.getKey() + " " + child.getValue().getCardinality());
            }
            sb.append(joiner);
        }

        final var attributes = node.getAttributes();
        if (attributes.isEmpty()) {
            if (children.isEmpty()) {
                sb.append("\n  [no children, no attributes]");
            } else {
                sb.append("\n  [no attributes]");
            }
        } else {
            for (final var attr : attributes.entrySet()) {
                final var info = attr.getValue();
                sb.append("\n  @").append(info.getName()).append(": ").append(info.getInferredType());
                final var samples = info.getSampleValues();
                if (!samples.isEmpty()) {
                    final var sampleJoiner = new StringJoiner(", ");
                    samples.forEach(sampleJoiner::add);
                    sb.append(" (samples: ").append(sampleJoiner).append(")");
                }
            }
        }

        if (node.isHasTextContent()) {
            sb.append("\n  [has text content]");
        } else {
            sb.append("\n  [no text]");
        }

        return sb.toString();
    }
}
