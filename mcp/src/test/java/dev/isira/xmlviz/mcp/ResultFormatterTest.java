package dev.isira.xmlviz.mcp;

import dev.isira.xmlviz.model.IndexEntry;
import dev.isira.xmlviz.model.ParseResult;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResultFormatterTest {

    @Test
    void formatElementDetailWithAttributes() {
        final var result = buildResult(
                entry(0, "root", 0, -1, Map.of()),
                entry(1, "member", 1, 0, Map.of("id", "101", "active", "true"))
        );
        final var detail = ResultFormatter.formatElementDetail(
                result.getInstanceIndex().get(1), result);
        assertTrue(detail.contains("Element #1: <member>"));
        assertTrue(detail.contains("Depth: 1"));
        assertTrue(detail.contains("Parent: #0 <root>"));
        assertTrue(detail.contains("id = \"101\""));
        assertTrue(detail.contains("active = \"true\""));
    }

    @Test
    void formatElementDetailWithText() {
        final var e = entry(0, "title", 0, -1, Map.of());
        e.setTextPreview("Hello World");
        final var result = buildResult(e);
        final var detail = ResultFormatter.formatElementDetail(
                result.getInstanceIndex().getFirst(), result);
        assertTrue(detail.contains("Text: \"Hello World\""));
        assertTrue(detail.contains("[truncated: no]"));
    }

    @Test
    void formatElementDetailWithTruncatedText() {
        final var e = entry(0, "body", 0, -1, Map.of());
        e.setTextPreview("Some long content...");
        e.setHasMoreText(true);
        final var result = buildResult(e);
        final var detail = ResultFormatter.formatElementDetail(
                result.getInstanceIndex().getFirst(), result);
        assertTrue(detail.contains("[truncated: yes]"));
    }

    @Test
    void formatElementDetailWithChildren() {
        final var result = buildResult(
                entry(0, "root", 0, -1, Map.of()),
                entry(1, "child1", 1, 0, Map.of()),
                entry(2, "child2", 1, 0, Map.of())
        );
        final var detail = ResultFormatter.formatElementDetail(
                result.getInstanceIndex().getFirst(), result);
        assertTrue(detail.contains("Children: 2"));
        assertTrue(detail.contains("Child elements:"));
        assertTrue(detail.contains("#1"));
        assertTrue(detail.contains("#2"));
    }

    @Test
    void formatSearchResultsEmpty() {
        final var result = buildResult(entry(0, "root", 0, -1, Map.of()));
        final var output = ResultFormatter.formatSearchResults(
                "notfound", List.of(), 0, 20, 0, result);
        assertTrue(output.contains("0 matches"));
    }

    @Test
    void formatSearchResultsWithMatches() {
        final var e = entry(0, "item", 0, -1, Map.of("name", "alice"));
        e.setTextPreview("Alice in wonderland");
        final var result = buildResult(e);
        final var output = ResultFormatter.formatSearchResults(
                "alice", List.of(0), 0, 20, 1, result);
        assertTrue(output.contains("1 matches"));
        assertTrue(output.contains("#0"));
        assertTrue(output.contains("xpath:"));
        assertTrue(output.contains("text: \"Alice in wonderland\""));
    }

    @Test
    void formatSearchResultsPagination() {
        final List<IndexEntry> entries = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            entries.add(entry(i, "item", 0, -1, Map.of("n", String.valueOf(i))));
        }
        final var result = buildResult(entries.toArray(new IndexEntry[0]));
        // Show page of IDs 0-19 out of 30 total
        final List<Integer> pageIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) pageIds.add(i);
        final var output = ResultFormatter.formatSearchResults(
                "item", pageIds, 0, 20, 30, result);
        assertTrue(output.contains("showing 1-20"));
        assertTrue(output.contains("Use offset=20 for next page"));
    }

    @Test
    void formatElementDetailRootHasNoParent() {
        final var result = buildResult(entry(0, "root", 0, -1, Map.of()));
        final var detail = ResultFormatter.formatElementDetail(
                result.getInstanceIndex().getFirst(), result);
        assertFalse(detail.contains("Parent:"));
    }

    private IndexEntry entry(int id, String tagName, int depth, int parentId, Map<String, String> attrs) {
        return new IndexEntry(id, tagName, depth, parentId, new LinkedHashMap<>(attrs));
    }

    private ParseResult buildResult(IndexEntry... entries) {
        final List<IndexEntry> index = new ArrayList<>();
        final Map<Integer, List<Integer>> childrenByParentId = new java.util.HashMap<>();
        for (final var e : entries) {
            while (index.size() <= e.getId()) {
                index.add(null);
            }
            index.set(e.getId(), e);
            childrenByParentId.computeIfAbsent(e.getParentId(), _ -> new ArrayList<>()).add(e.getId());
        }
        for (final var e : entries) {
            final var children = childrenByParentId.getOrDefault(e.getId(), List.of());
            e.setChildCount(children.size());
        }
        return new ParseResult(Map.of(), index, childrenByParentId, entries.length);
    }
}
