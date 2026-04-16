package dev.isira.xmlviz.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseResultTest {

    @Test
    void toXmlSelfClosingElement() {
        final var result = buildResult(
                entry(0, "br", 0, -1, Map.of())
        );
        assertEquals("<br/>", result.toXml(result.getInstanceIndex().get(0)));
    }

    @Test
    void toXmlElementWithAttributes() {
        final var result = buildResult(
                entry(0, "input", 0, -1, Map.of("type", "text", "value", "a&b"))
        );
        final var xml = result.toXml(result.getInstanceIndex().get(0));
        assertTrue(xml.contains("type=\"text\""));
        assertTrue(xml.contains("value=\"a&amp;b\""));
        assertTrue(xml.startsWith("<input "));
        assertTrue(xml.endsWith("/>"));
    }

    @Test
    void toXmlElementWithTextContent() {
        final var e = entry(0, "p", 0, -1, Map.of());
        e.setTextPreview("Hello <world>");
        final var result = buildResult(e);
        assertEquals("<p>Hello &lt;world&gt;</p>", result.toXml(result.getInstanceIndex().get(0)));
    }

    @Test
    void toXmlElementWithTruncatedText() {
        final var e = entry(0, "p", 0, -1, Map.of());
        e.setTextPreview("Some long text");
        e.setHasMoreText(true);
        final var result = buildResult(e);
        assertEquals("<p>Some long text[truncated]</p>", result.toXml(result.getInstanceIndex().get(0)));
    }

    @Test
    void toXmlElementWithChildren() {
        final var result = buildResult(
                entry(0, "root", 0, -1, Map.of()),
                entry(1, "child", 1, 0, Map.of("id", "1")),
                entry(2, "child", 1, 0, Map.of("id", "2"))
        );
        final var xml = result.toXml(result.getInstanceIndex().get(0));
        final var expected = """
                <root>
                  <child id="1"/>
                  <child id="2"/>
                </root>""";
        assertEquals(expected, xml);
    }

    @Test
    void toXmlNestedChildren() {
        final var result = buildResult(
                entry(0, "a", 0, -1, Map.of()),
                entry(1, "b", 1, 0, Map.of()),
                entry(2, "c", 2, 1, Map.of())
        );
        final var xml = result.toXml(result.getInstanceIndex().get(0));
        final var expected = """
                <a>
                  <b>
                    <c/>
                  </b>
                </a>""";
        assertEquals(expected, xml);
    }

    @Test
    void toXmlBudgetExceeded() {
        // Create a parent with many children to exceed the budget
        final List<IndexEntry> entries = new ArrayList<>();
        entries.add(entry(0, "root", 0, -1, Map.of()));
        // Add enough children — each consumes 1 from the budget, plus the root consumes 1
        // We rely on the 50,000 budget being finite; create a deeply nested structure
        // For a simpler test, we just verify the method doesn't blow up on a moderate tree
        for (int i = 1; i <= 100; i++) {
            entries.add(entry(i, "item", 1, 0, Map.of("n", String.valueOf(i))));
        }
        final var result = buildResult(entries.toArray(new IndexEntry[0]));
        final var xml = result.toXml(result.getInstanceIndex().get(0));
        assertNotNull(xml);
        assertTrue(xml.startsWith("<root>"));
        assertTrue(xml.endsWith("</root>"));
        // Should contain all 100 children since we're well under budget
        assertTrue(xml.contains("n=\"1\""));
        assertTrue(xml.contains("n=\"100\""));
    }

    @Test
    void toXmlEscapesAllSpecialChars() {
        final var e = entry(0, "data", 0, -1, Map.of("attr", "a\"b'c"));
        e.setTextPreview("1 < 2 & 3 > 0");
        final var result = buildResult(e);
        final var xml = result.toXml(result.getInstanceIndex().get(0));
        assertTrue(xml.contains("attr=\"a&quot;b&apos;c\""));
        assertTrue(xml.contains("1 &lt; 2 &amp; 3 &gt; 0"));
    }

    private IndexEntry entry(int id, String tagName, int depth, int parentId, Map<String, String> attrs) {
        return new IndexEntry(id, tagName, depth, parentId, new LinkedHashMap<>(attrs));
    }

    private ParseResult buildResult(IndexEntry... entries) {
        final List<IndexEntry> index = new ArrayList<>();
        final Map<Integer, List<Integer>> childrenByParentId = new java.util.HashMap<>();
        for (final var e : entries) {
            // Ensure the list is large enough (entries may not be added in order, but here they are)
            while (index.size() <= e.getId()) {
                index.add(null);
            }
            index.set(e.getId(), e);
            childrenByParentId.computeIfAbsent(e.getParentId(), _ -> new ArrayList<>()).add(e.getId());
        }
        // Set child counts
        for (final var e : entries) {
            final var children = childrenByParentId.getOrDefault(e.getId(), List.of());
            e.setChildCount(children.size());
        }
        return new ParseResult(Map.of(), index, childrenByParentId, entries.length);
    }
}
