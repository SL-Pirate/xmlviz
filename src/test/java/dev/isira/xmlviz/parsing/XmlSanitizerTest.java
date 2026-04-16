package dev.isira.xmlviz.parsing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XmlSanitizerTest {

    @TempDir
    Path tempDir;

    @Test
    void escapesBareAmpersands() throws Exception {
        final var file = writeFile("<root><item>a&b</item></root>");
        final var sanitizer = new XmlSanitizer();
        final var result = sanitizer.sanitize(file.toFile(), null);
        final var content = Files.readString(result.toPath());
        assertTrue(content.contains("a&amp;b"));
    }

    @Test
    void preservesValidEntities() throws Exception {
        final var xml = "<root>&amp; &lt; &gt; &quot; &apos;</root>";
        final var file = writeFile(xml);
        final var result = new XmlSanitizer().sanitize(file.toFile(), null);
        final var content = Files.readString(result.toPath());
        assertTrue(content.contains("&amp; &lt; &gt; &quot; &apos;"));
    }

    @Test
    void preservesNumericEntities() throws Exception {
        final var xml = "<root>&#169; &#xA9;</root>";
        final var file = writeFile(xml);
        final var result = new XmlSanitizer().sanitize(file.toFile(), null);
        final var content = Files.readString(result.toPath());
        assertTrue(content.contains("&#169;"));
        assertTrue(content.contains("&#xA9;"));
    }

    @Test
    void handlesMixedContent() throws Exception {
        final var xml = "<root>foo&bar&amp;baz</root>";
        final var file = writeFile(xml);
        final var result = new XmlSanitizer().sanitize(file.toFile(), null);
        final var content = Files.readString(result.toPath());
        assertTrue(content.contains("foo&amp;bar&amp;baz"));
    }

    @Test
    void fixesUrlsWithBareAmpersands() throws Exception {
        final var xml = "<root><link>http://example.com?a=1&amp;b=2&c=3</link></root>";
        final var file = writeFile(xml);
        final var result = new XmlSanitizer().sanitize(file.toFile(), null);
        final var content = Files.readString(result.toPath());
        assertTrue(content.contains("a=1&amp;b=2&amp;c=3"));
    }

    @Test
    void sanitizedOutputIsParseableByStax() throws Exception {
        final var xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss>
                    <channel>
                        <item>
                            <link>http://example.com?foo=1&bar=2</link>
                            <title>Test & Title</title>
                            <guid>http://example.com?x=1&y=2&z=3</guid>
                        </item>
                    </channel>
                </rss>
                """;
        final var file = writeFile(xml);
        final var result = new XmlSanitizer().sanitize(file.toFile(), null);

        // Parse with StAX — should not throw
        final var factory = XMLInputFactory.newInstance();
        try (final var fis = new FileInputStream(result)) {
            final var reader = factory.createXMLStreamReader(fis);
            int elementCount = 0;
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                    elementCount++;
                }
            }
            assertEquals(6, elementCount); // rss, channel, item, link, title, guid
            reader.close();
        }
    }

    @Test
    void originalFileIsNotModified() throws Exception {
        final var xml = "<root>a&b</root>";
        final var file = writeFile(xml);
        final var originalContent = Files.readString(file);
        new XmlSanitizer().sanitize(file.toFile(), null);
        assertEquals(originalContent, Files.readString(file));
    }

    @Test
    void reportsProgress() throws Exception {
        final var sb = new StringBuilder("<root>");
        for (int i = 0; i < 1000; i++) {
            sb.append("<item>value&other</item>\n");
        }
        sb.append("</root>");
        final var file = writeFile(sb.toString());

        double[] lastProgress = {0};
        new XmlSanitizer().sanitize(file.toFile(), p -> lastProgress[0] = p);
        assertTrue(lastProgress[0] > 0, "Progress should have been reported");
    }

    private Path writeFile(String content) throws Exception {
        final var file = tempDir.resolve("test.xml");
        Files.writeString(file, content);
        return file;
    }
}
