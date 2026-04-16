# xmlviz

A desktop XML structure visualizer that handles large files (80MB+) without running out of memory. Instead of rendering every element, it infers the implicit schema from the data and displays it as an entity-relationship diagram.

Built with Java 25 and JavaFX.

## Features

### Schema / ERD View

Infers the structure of an XML file and renders it as an entity-relationship diagram:

- Each distinct element tag becomes an entity box showing its attributes and their inferred data types (string, integer, decimal, boolean, date)
- Parent-child relationships are drawn as directed edges with cardinality labels (`1`, `0..1`, `1..*`, `0..*`) computed from actual occurrence counts
- Ctrl+scroll to zoom, drag to pan
- Even an 80MB file typically has only ~20-100 distinct element types, so the ERD is always compact

### Instance Tree View

A lazy-expanding tree showing every element instance in the file:

- Nodes display tag name, key attributes, child count, and a text preview
- Expand on demand — only loads children when you click
- Select a node to see full details: all attributes, text content, XPath, depth

### Malformed XML Handling

When parsing fails due to common issues like unescaped ampersands (typical in WordPress WXR exports), xmlviz offers to auto-fix the file and retry — no manual editing needed.

### Other

- Drag-and-drop XML files onto the window
- Ctrl+O to open files
- Background parsing with progress bar
- Encoding detection from XML declaration

## Requirements

- Java 25+
- JavaFX 21+ (pulled automatically via Gradle)

## Build & Run

```bash
./gradlew run
```

To pass a file directly:

```bash
./gradlew run --args="/path/to/file.xml"
```

### Run Tests

```bash
./gradlew test
```

## Architecture

**Single-pass StAX parsing** — the file is read once with a streaming pull-parser (never loaded into DOM), building two data structures simultaneously:

1. **SchemaMap** — one `SchemaNode` per distinct tag name, tracking attributes, child types, occurrence counts, and cardinality min/max
2. **InstanceIndex** — a flat `ArrayList<IndexEntry>` with parent-child ID references for O(1) tree navigation

The parser runs on a background thread. The ERD view uses plain JavaFX shapes with a BFS-based hierarchical layout. The tree view uses JavaFX `TreeView` with lazy-loading `TreeItem` nodes.

## Project Structure

```
src/main/java/dev/isira/xmlviz/
├── XmlVizApp.java                  # Application entry point, UI wiring
├── model/
│   ├── SchemaNode.java             # Element type schema (attributes, children, cardinality)
│   ├── ChildInfo.java              # Cardinality tracking (min/max occurrences)
│   ├── AttributeInfo.java          # Attribute metadata with type inference
│   ├── IndexEntry.java             # Per-element instance index entry
│   └── ParseResult.java            # Combined parse output with XPath builder
├── parsing/
│   ├── XmlParser.java              # Single-pass StAX parser
│   ├── XmlSanitizer.java           # Streaming fix for malformed XML
│   └── CountingInputStream.java    # Byte-counting stream wrapper
└── ui/
    ├── ErdView.java                # Schema/ERD visualization
    └── InstanceTreeView.java       # Lazy-loading tree with detail panel
```

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

## Acknowledgements

Inspired by JetBrains' [DataGraph](https://plugins.jetbrains.com/plugin/22472-datagraph) plugin — xmlviz aims to provide similar structural visualization as a free, standalone desktop app.

Built with the assistance of [Claude Code](https://claude.ai/claude-code) by Anthropic.
