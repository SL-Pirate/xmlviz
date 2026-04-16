package dev.isira.xmlviz.mcp;

import dev.isira.xmlviz.model.IndexEntry;
import dev.isira.xmlviz.model.ParseResult;

import java.io.File;
import java.util.List;

public final class ResultFormatter {

    private ResultFormatter() {
    }

    public static String formatParseSummary(File file, ParseResult result) {
        final var sb = new StringBuilder();
        sb.append("Parsed: ").append(file.getAbsolutePath());
        sb.append(" (").append(formatFileSize(file.length())).append(")\n");
        sb.append("Total elements: ").append(formatNumber(result.getTotalElements())).append("\n");
        sb.append("Unique element types: ").append(result.getSchemaMap().size()).append("\n");

        final var roots = result.getRootEntries();
        if (roots.size() == 1) {
            sb.append("Root element: <").append(roots.getFirst().getTagName()).append(">");
        } else {
            sb.append("Root elements: ").append(roots.size());
        }
        return sb.toString();
    }

    public static String formatSearchResults(String query, List<Integer> matchIds, int offset, int limit,
                                             int totalMatches, ParseResult result) {
        final var sb = new StringBuilder();
        sb.append("Search: \"").append(query).append("\" — ");
        sb.append(formatNumber(totalMatches)).append(totalMatches >= 10_000 ? "+" : "").append(" matches");

        if (matchIds.isEmpty()) {
            return sb.toString();
        }

        final var end = Math.min(offset + limit, totalMatches);
        sb.append(" (showing ").append(offset + 1).append("-").append(end).append(")");

        for (int i = 0; i < matchIds.size(); i++) {
            final var entryId = matchIds.get(i);
            final var entry = result.getInstanceIndex().get(entryId);
            sb.append("\n\n[").append(offset + i).append("] ");
            sb.append("#").append(entryId).append(" ");
            sb.append(entry.toString());
            sb.append(" depth=").append(entry.getDepth());
            sb.append("\n    xpath: ").append(result.buildXPath(entry));
            if (entry.getTextPreview() != null && !entry.getTextPreview().isEmpty()) {
                sb.append("\n    text: \"").append(entry.getTextPreview()).append("\"");
                if (entry.isHasMoreText()) {
                    sb.append("[truncated]");
                }
            }
        }

        if (end < totalMatches) {
            sb.append("\n\n(Use offset=").append(end).append(" for next page)");
        }
        return sb.toString();
    }

    public static String formatElementDetail(IndexEntry entry, ParseResult result) {
        final var sb = new StringBuilder();
        sb.append("Element #").append(entry.getId()).append(": <").append(entry.getTagName()).append(">\n");
        sb.append("XPath: ").append(result.buildXPath(entry)).append("\n");
        sb.append("Depth: ").append(entry.getDepth());
        sb.append(", Children: ").append(entry.getChildCount());

        if (entry.getParentId() >= 0) {
            final var parent = result.getInstanceIndex().get(entry.getParentId());
            sb.append(", Parent: #").append(entry.getParentId()).append(" <").append(parent.getTagName()).append(">");
        }

        final var attributes = entry.getAttributes();
        if (!attributes.isEmpty()) {
            sb.append("\nAttributes:");
            for (final var attr : attributes.entrySet()) {
                sb.append("\n  ").append(attr.getKey()).append(" = \"").append(attr.getValue()).append("\"");
            }
        }

        if (entry.getTextPreview() != null && !entry.getTextPreview().isEmpty()) {
            sb.append("\nText: \"").append(entry.getTextPreview()).append("\"");
            sb.append(" [truncated: ").append(entry.isHasMoreText() ? "yes" : "no").append("]");
        }

        final var childIds = result.getChildIds(entry.getId());
        if (!childIds.isEmpty()) {
            sb.append("\n\nChild elements:");
            final var shown = Math.min(childIds.size(), 50);
            for (int i = 0; i < shown; i++) {
                final var childId = childIds.get(i);
                final var child = result.getInstanceIndex().get(childId);
                sb.append("\n  #").append(childId).append(" ").append(child.toString());
            }
            if (childIds.size() > 50) {
                sb.append("\n  ... and ").append(childIds.size() - 50).append(" more");
            }
        }
        return sb.toString();
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String formatNumber(long n) {
        return String.format("%,d", n);
    }
}
