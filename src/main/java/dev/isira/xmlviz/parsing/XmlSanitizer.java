package dev.isira.xmlviz.parsing;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Slf4j
public class XmlSanitizer {

    private static final Pattern BARE_AMPERSAND = Pattern.compile(
            "&(?!amp;|lt;|gt;|quot;|apos;|#\\d+;|#x[0-9a-fA-F]+;)"
    );

    public File sanitize(File input, Consumer<Double> progressCallback) throws IOException {
        final var fileSize = input.length();
        final var charset = Charset.forName(new XmlParser().detectEncoding(input));
        final var output = Files.createTempFile("xmlviz-sanitized-", ".xml");
        long bytesProcessed = 0;

        try (final var reader = new BufferedReader(new InputStreamReader(new FileInputStream(input), charset), 65536);
             final var writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output.toFile()), charset), 65536)) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(BARE_AMPERSAND.matcher(line).replaceAll("&amp;"));
                writer.newLine();
                bytesProcessed += line.length() + 1;
                if (progressCallback != null && fileSize > 0) {
                    progressCallback.accept(Math.min((double) bytesProcessed / fileSize, 1.0));
                }
            }
        }

        log.info("Sanitized {} -> {}", input.getName(), output);
        return output.toFile();
    }
}
