package dev.isira.xmlviz.mcp;

import dev.isira.xmlviz.model.ParseResult;
import dev.isira.xmlviz.parsing.XmlParser;
import dev.isira.xmlviz.parsing.XmlSanitizer;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class XmlVizMcpServer {
    private ParseResult currentResult;

    @SuppressWarnings({"UnnecessaryModifier", "unused"})
    public static void main(String[] args) {
        final var server = new XmlVizMcpServer();
        server.start();
    }

    private static McpSchema.Tool parseXmlTool() {
        return McpSchema.Tool.builder()
                .name("parse_xml")
                .description("Parse an XML file and return a structural summary. Call this first before using other tools.")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "filePath", Map.of("type", "string",
                                        "description", "Absolute path to the XML file"),
                                "sanitize", Map.of("type", "boolean",
                                        "description", "Auto-fix bare ampersands for malformed XML (e.g. WordPress WXR exports). Default: false")
                        ),
                        List.of("filePath"),
                        null, null, null))
                .build();
    }

    // --- Tool definitions ---

    private static McpSchema.Tool getSchemaTool() {
        return McpSchema.Tool.builder()
                .name("get_schema")
                .description("Return the inferred schema of the parsed XML file — all element types, their attributes (with inferred types and sample values), child relationships with cardinalities, and instance counts. This gives complete structural understanding regardless of file size.")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "elementType", Map.of("type", "string",
                                        "description", "Filter to a specific element type name. Omit for the full schema.")
                        ),
                        List.of(),
                        null, null, null))
                .build();
    }

    private static McpSchema.Tool searchTool() {
        return McpSchema.Tool.builder()
                .name("search")
                .description("Search for elements by text. Searches tag names, attribute values, and text content (case-insensitive). Returns element IDs, XPath paths, and previews.")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "query", Map.of("type", "string",
                                        "description", "Case-insensitive search term"),
                                "limit", Map.of("type", "integer",
                                        "description", "Max results to return (default 20, max 100)"),
                                "offset", Map.of("type", "integer",
                                        "description", "Skip first N results for pagination (default 0)")
                        ),
                        List.of("query"),
                        null, null, null))
                .build();
    }

    private static McpSchema.Tool getElementTool() {
        return McpSchema.Tool.builder()
                .name("get_element")
                .description("Get full details of a specific element by its index ID (from search results or navigation). Shows XPath, attributes, text preview, parent, and immediate children.")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "elementId", Map.of("type", "integer",
                                        "description", "Element index ID")
                        ),
                        List.of("elementId"),
                        null, null, null))
                .build();
    }

    private static McpSchema.Tool getSubtreeTool() {
        return McpSchema.Tool.builder()
                .name("get_subtree")
                .description("Reconstruct the XML subtree rooted at a given element. Text content is truncated to 200 chars per element (as stored during parsing).")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "elementId", Map.of("type", "integer",
                                        "description", "Root element index ID for subtree extraction"),
                                "maxNodes", Map.of("type", "integer",
                                        "description", "Maximum nodes in output (default 1000, max 50000)")
                        ),
                        List.of("elementId"),
                        null, null, null))
                .build();
    }

    private static McpSchema.CallToolResult textResult(String text) {
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(text)))
                .build();
    }

    // --- Tool handlers ---

    private static McpSchema.CallToolResult errorResult(String message) {
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent("Error: " + message)))
                .isError(true)
                .build();
    }

    private static int getIntArg(McpSchema.CallToolRequest request, String key, int defaultValue) {
        final var value = request.arguments().get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return defaultValue;
    }

    private void start() {
        final var jsonMapper = new JacksonMcpJsonMapperSupplier().get();
        final var transportProvider = new StdioServerTransportProvider(jsonMapper);

        final McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("xmlviz", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(false)
                        .build())
                .toolCall(
                        parseXmlTool(),
                        (_, request) -> handleParseXml(request)
                )
                .toolCall(
                        getSchemaTool(),
                        (_, request) -> handleGetSchema(request)
                )
                .toolCall(
                        searchTool(),
                        (_, request) -> handleSearch(request)
                )
                .toolCall(
                        getElementTool(),
                        (_, request) -> handleGetElement(request)
                )
                .toolCall(
                        getSubtreeTool(),
                        (_, request) -> handleGetSubtree(request)
                )
                .build();

        log.info("xmlviz MCP server started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down xmlviz MCP server");
            server.close();
        }));
    }

    private McpSchema.CallToolResult handleParseXml(McpSchema.CallToolRequest request) {
        final var filePath = (String) request.arguments().get("filePath");
        final var sanitize = Boolean.TRUE.equals(request.arguments().get("sanitize"));

        if (filePath == null || filePath.isBlank()) {
            return errorResult("filePath is required");
        }

        var file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return errorResult("File not found: " + filePath);
        }

        try {
            if (sanitize) {
                log.info("Sanitizing XML file: {}", filePath);
                file = new XmlSanitizer().sanitize(file, _ -> {
                });
            }

            log.info("Parsing XML file: {}", filePath);
            final var parser = new XmlParser();
            currentResult = parser.parse(file, _ -> {
            });
            File currentFile = new File(filePath);

            final var summary = ResultFormatter.formatParseSummary(currentFile, currentResult);
            return textResult(summary);
        } catch (Exception e) {
            log.error("Failed to parse XML file", e);
            return errorResult("Failed to parse: " + e.getMessage());
        }
    }

    private McpSchema.CallToolResult handleGetSchema(McpSchema.CallToolRequest request) {
        if (currentResult == null) {
            return errorResult("No file parsed yet. Call parse_xml first.");
        }

        final var elementType = (String) request.arguments().get("elementType");
        if (elementType != null && !elementType.isBlank()) {
            final var node = currentResult.getSchemaMap().get(elementType);
            if (node == null) {
                return errorResult("Unknown element type: " + elementType
                        + ". Known types: " + String.join(", ", currentResult.getSchemaMap().keySet()));
            }
            return textResult(SchemaFormatter.formatNode(node));
        }

        return textResult(SchemaFormatter.formatAll(currentResult.getSchemaMap()));
    }

    // --- Helpers ---

    private McpSchema.CallToolResult handleSearch(McpSchema.CallToolRequest request) {
        if (currentResult == null) {
            return errorResult("No file parsed yet. Call parse_xml first.");
        }

        final var query = (String) request.arguments().get("query");
        if (query == null || query.isBlank()) {
            return errorResult("query is required");
        }

        final var limit = Math.min(getIntArg(request, "limit", 20), 100);
        final var offset = Math.max(getIntArg(request, "offset", 0), 0);
        final var normalizedQuery = query.strip().toLowerCase();

        final var index = currentResult.getInstanceIndex();
        final List<Integer> allMatches = new ArrayList<>(1024);
        for (int i = 0, size = index.size(); i < size; i++) {
            final var entry = index.get(i);
            if (entry != null && entry.matches(normalizedQuery)) {
                allMatches.add(i);
                if (allMatches.size() >= 10_000) break;
            }
        }

        final var totalMatches = allMatches.size();
        final var pageStart = Math.min(offset, totalMatches);
        final var pageEnd = Math.min(pageStart + limit, totalMatches);
        final List<Integer> page = allMatches.subList(pageStart, pageEnd);

        return textResult(ResultFormatter.formatSearchResults(
                query, page, pageStart, limit, totalMatches, currentResult));
    }

    @SuppressWarnings("DuplicatedCode")
    private McpSchema.CallToolResult handleGetElement(McpSchema.CallToolRequest request) {
        if (currentResult == null) {
            return errorResult("No file parsed yet. Call parse_xml first.");
        }

        final var elementId = getIntArg(request, "elementId", -1);
        final var index = currentResult.getInstanceIndex();

        if (elementId < 0 || elementId >= index.size() || index.get(elementId) == null) {
            return errorResult("Invalid elementId: " + elementId
                    + ". Valid range: 0-" + (index.size() - 1));
        }

        final var entry = index.get(elementId);
        return textResult(ResultFormatter.formatElementDetail(entry, currentResult));
    }

    @SuppressWarnings("DuplicatedCode")
    private McpSchema.CallToolResult handleGetSubtree(McpSchema.CallToolRequest request) {
        if (currentResult == null) {
            return errorResult("No file parsed yet. Call parse_xml first.");
        }

        final var elementId = getIntArg(request, "elementId", -1);
        final var index = currentResult.getInstanceIndex();

        if (elementId < 0 || elementId >= index.size() || index.get(elementId) == null) {
            return errorResult("Invalid elementId: " + elementId
                    + ". Valid range: 0-" + (index.size() - 1));
        }

        final var maxNodes = Math.clamp(getIntArg(request, "maxNodes", 1000), 1, 50_000);
        final var entry = index.get(elementId);
        final var xml = currentResult.toXml(entry, maxNodes);
        return textResult(xml);
    }
}
