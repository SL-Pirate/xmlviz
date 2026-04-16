package dev.isira.xmlviz.parsing;

import dev.isira.xmlviz.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XmlParserTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesBasicStructure() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <catalog>
                    <book id="1" isbn="978-0-123">
                        <title>Java in Action</title>
                        <author>John Doe</author>
                        <price>29.99</price>
                    </book>
                    <book id="2" isbn="978-0-456">
                        <title>XML Mastery</title>
                        <author>Jane Smith</author>
                        <author>Bob Lee</author>
                        <price>39.99</price>
                        <tags>
                            <tag>xml</tag>
                            <tag>data</tag>
                        </tags>
                    </book>
                    <book id="3" isbn="978-0-789">
                        <title>Design Patterns</title>
                        <price>49.99</price>
                    </book>
                </catalog>
                """;

        File file = tempDir.resolve("test.xml").toFile();
        Files.writeString(file.toPath(), xml);

        XmlParser parser = new XmlParser();
        ParseResult result = parser.parse(file, null);

        // Total elements
        assertEquals(16, result.getTotalElements());

        // Schema map should have: catalog, book, title, author, price, tags, tag
        var schema = result.getSchemaMap();
        assertEquals(7, schema.size());
        assertTrue(schema.containsKey("catalog"));
        assertTrue(schema.containsKey("book"));
        assertTrue(schema.containsKey("title"));
        assertTrue(schema.containsKey("author"));
        assertTrue(schema.containsKey("price"));
        assertTrue(schema.containsKey("tags"));
        assertTrue(schema.containsKey("tag"));

        // catalog has 1 instance
        assertEquals(1, schema.get("catalog").getInstanceCount());
        // book has 3 instances
        assertEquals(3, schema.get("book").getInstanceCount());
        // author has 3 instances
        assertEquals(3, schema.get("author").getInstanceCount());

        // book attributes: id (integer) and isbn (string)
        SchemaNode bookSchema = schema.get("book");
        assertEquals(2, bookSchema.getAttributes().size());
        assertEquals("integer", bookSchema.getAttributes().get("id").getInferredType());
        assertEquals("string", bookSchema.getAttributes().get("isbn").getInferredType());

        // price has text content
        assertTrue(schema.get("price").isHasTextContent());

        // Cardinality: catalog -> book is 1..* (always 3 in this case, but min=3, max=3 => "1..*" since min>=1 and max>1)
        ChildInfo catalogToBook = schema.get("catalog").getChildren().get("book");
        assertNotNull(catalogToBook);
        assertEquals(3, catalogToBook.getMinOccurrences());
        assertEquals(3, catalogToBook.getMaxOccurrences());
        assertEquals("1..*", catalogToBook.getCardinality());

        // Cardinality: book -> author is 0..* (0 in book#3, 1 in book#1, 2 in book#2)
        ChildInfo bookToAuthor = schema.get("book").getChildren().get("author");
        assertNotNull(bookToAuthor);
        assertEquals(0, bookToAuthor.getMinOccurrences());
        assertEquals(2, bookToAuthor.getMaxOccurrences());
        assertEquals("0..*", bookToAuthor.getCardinality());

        // Cardinality: book -> tags is 0..1
        ChildInfo bookToTags = schema.get("book").getChildren().get("tags");
        assertNotNull(bookToTags);
        assertEquals(0, bookToTags.getMinOccurrences());
        assertEquals(1, bookToTags.getMaxOccurrences());
        assertEquals("0..1", bookToTags.getCardinality());

        // Instance index - root entries
        var roots = result.getRootEntries();
        assertEquals(1, roots.size());
        assertEquals("catalog", roots.getFirst().getTagName());

        // Root has 3 children (books)
        IndexEntry catalogEntry = roots.getFirst();
        assertEquals(3, catalogEntry.getChildCount());

        // XPath of first book
        var bookIds = result.getChildIds(catalogEntry.getId());
        assertEquals(3, bookIds.size());
        IndexEntry firstBook = result.getInstanceIndex().get(bookIds.getFirst());
        assertEquals("book", firstBook.getTagName());
        assertEquals("/catalog[1]/book[1]", result.buildXPath(firstBook));

        // XPath of second book
        IndexEntry secondBook = result.getInstanceIndex().get(bookIds.get(1));
        assertEquals("/catalog[1]/book[2]", result.buildXPath(secondBook));

        // Text content of title
        var firstBookChildren = result.getChildIds(firstBook.getId());
        IndexEntry titleEntry = result.getInstanceIndex().get(firstBookChildren.getFirst());
        assertEquals("title", titleEntry.getTagName());
        assertEquals("Java in Action", titleEntry.getTextPreview());
    }

    @Test
    void handlesEmptyXml() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root/>";
        File file = tempDir.resolve("empty.xml").toFile();
        Files.writeString(file.toPath(), xml);

        XmlParser parser = new XmlParser();
        ParseResult result = parser.parse(file, null);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getSchemaMap().size());
        assertTrue(result.getSchemaMap().containsKey("root"));
    }

    @Test
    void infersAttributeTypes() throws Exception {
        String xml = """
                <data>
                    <record id="1" score="3.14" active="true" date="2024-01-15" name="Alice"/>
                    <record id="2" score="2.71" active="false" date="2024-02-20" name="Bob"/>
                </data>
                """;
        File file = tempDir.resolve("types.xml").toFile();
        Files.writeString(file.toPath(), xml);

        ParseResult result = new XmlParser().parse(file, null);
        var attrs = result.getSchemaMap().get("record").getAttributes();

        assertEquals("integer", attrs.get("id").getInferredType());
        assertEquals("decimal", attrs.get("score").getInferredType());
        assertEquals("boolean", attrs.get("active").getInferredType());
        assertEquals("date", attrs.get("date").getInferredType());
        assertEquals("string", attrs.get("name").getInferredType());
    }

    @Test
    void reportsProgress() throws Exception {
        StringBuilder xml = new StringBuilder("<root>");
        for (int i = 0; i < 50_000; i++) {
            xml.append("<item id=\"").append(i).append("\"/>");
        }
        xml.append("</root>");

        File file = tempDir.resolve("large.xml").toFile();
        Files.writeString(file.toPath(), xml.toString());

        double[] lastProgress = {0};
        ParseResult result = new XmlParser().parse(file, p -> lastProgress[0] = p);

        assertEquals(50_001, result.getTotalElements());
        assertTrue(lastProgress[0] > 0, "Progress should have been reported");
    }
}
