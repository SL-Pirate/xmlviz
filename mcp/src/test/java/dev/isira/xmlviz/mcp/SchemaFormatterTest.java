package dev.isira.xmlviz.mcp;

import dev.isira.xmlviz.model.SchemaNode;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaFormatterTest {

    @Test
    void formatNodeWithNoChildrenNoAttributes() {
        final var node = new SchemaNode("br");
        node.incrementInstanceCount();
        final var output = SchemaFormatter.formatNode(node);
        assertTrue(output.contains("br (1 instance)"));
        assertTrue(output.contains("[no children, no attributes]"));
        assertTrue(output.contains("[no text]"));
    }

    @Test
    void formatNodeWithAttributes() {
        final var node = new SchemaNode("input");
        node.incrementInstanceCount();
        node.incrementInstanceCount();
        node.addAttribute("type", "text");
        node.addAttribute("type", "number");
        node.addAttribute("value", "hello");
        final var output = SchemaFormatter.formatNode(node);
        assertTrue(output.contains("input (2 instances)"));
        assertTrue(output.contains("@type: string"));
        assertTrue(output.contains("@value: string"));
        assertTrue(output.contains("samples:"));
    }

    @Test
    void formatNodeWithChildren() {
        final var node = new SchemaNode("catalog");
        node.incrementInstanceCount();
        node.updateChildOccurrence("book", 5);
        node.updateChildOccurrence("metadata", 1);
        final var output = SchemaFormatter.formatNode(node);
        assertTrue(output.contains("children: book"));
        assertTrue(output.contains("metadata"));
        assertTrue(output.contains("[no attributes]"));
    }

    @Test
    void formatNodeWithTextContent() {
        final var node = new SchemaNode("title");
        node.incrementInstanceCount();
        node.setHasTextContent(true);
        final var output = SchemaFormatter.formatNode(node);
        assertTrue(output.contains("[has text content]"));
        assertFalse(output.contains("[no text]"));
    }

    @Test
    void formatAllMultipleNodes() {
        final Map<String, SchemaNode> schema = new LinkedHashMap<>();
        final var catalog = new SchemaNode("catalog");
        catalog.incrementInstanceCount();
        catalog.updateChildOccurrence("book", 3);
        schema.put("catalog", catalog);

        final var book = new SchemaNode("book");
        book.incrementInstanceCount();
        book.incrementInstanceCount();
        book.incrementInstanceCount();
        book.addAttribute("id", "1");
        schema.put("book", book);

        final var output = SchemaFormatter.formatAll(schema);
        assertTrue(output.contains("catalog (1 instance)"));
        assertTrue(output.contains("book (3 instances)"));
        // Nodes separated by blank line
        assertTrue(output.contains("\n\n"));
    }

    @Test
    void formatNodeInferredTypes() {
        final var node = new SchemaNode("price");
        node.incrementInstanceCount();
        node.addAttribute("amount", "19.99");
        node.addAttribute("active", "true");
        final var output = SchemaFormatter.formatNode(node);
        assertTrue(output.contains("@amount: decimal"));
        assertTrue(output.contains("@active: boolean"));
    }
}
