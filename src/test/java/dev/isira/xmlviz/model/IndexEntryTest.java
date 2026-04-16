package dev.isira.xmlviz.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndexEntryTest {

    @Test
    void matchesTagName() {
        final var entry = makeEntry("person", Map.of(), null);
        assertTrue(entry.matches("person"));
        assertTrue(entry.matches("pers"));
    }

    @Test
    void matchesTagNameCaseInsensitive() {
        final var entry = makeEntry("Person", Map.of(), null);
        assertTrue(entry.matches("person"));
    }

    @Test
    void matchesAttributeValue() {
        final var entry = makeEntry("item", Map.of("id", "ABC123", "type", "Widget"), null);
        assertTrue(entry.matches("abc123"));
        assertTrue(entry.matches("widget"));
        assertTrue(entry.matches("abc"));
    }

    @Test
    void doesNotMatchAttributeKeys() {
        final var entry = makeEntry("item", Map.of("secretKey", "value"), null);
        assertFalse(entry.matches("secretkey"));
    }

    @Test
    void matchesTextPreview() {
        final var entry = makeEntry("p", Map.of(), "Hello World");
        assertTrue(entry.matches("hello"));
        assertTrue(entry.matches("world"));
    }

    @Test
    void returnsFalseWhenNoMatch() {
        final var entry = makeEntry("item", Map.of("id", "123"), "some text");
        assertFalse(entry.matches("zzz"));
    }

    @Test
    void handlesNullTextPreview() {
        final var entry = makeEntry("item", Map.of(), null);
        assertFalse(entry.matches("anything"));
    }

    private IndexEntry makeEntry(String tagName, Map<String, String> attrs, String textPreview) {
        final var entry = new IndexEntry(0, tagName, 0, -1, new LinkedHashMap<>(attrs));
        entry.setTextPreview(textPreview);
        return entry;
    }
}
