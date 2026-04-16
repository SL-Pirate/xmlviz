package dev.isira.xmlviz.ui;

import dev.isira.xmlviz.model.AttributeInfo;
import dev.isira.xmlviz.model.ChildInfo;
import dev.isira.xmlviz.model.ParseResult;
import dev.isira.xmlviz.model.SchemaNode;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;

public class ErdView extends ScrollPane {

    private static final double NODE_WIDTH = 240;
    private static final double H_GAP = 80;
    private static final double V_GAP = 100;
    private static final double PADDING = 40;

    private final Pane canvas = new Pane();
    private final Map<String, Region> nodeBoxes = new HashMap<>();
    private final Map<String, double[]> nodePositions = new HashMap<>();

    public ErdView() {
        setContent(canvas);
        setPannable(true);
        setFitToWidth(false);
        setFitToHeight(false);
        canvas.setStyle("-fx-background-color: #f8f9fa;");

        canvas.setOnScroll(event -> {
            if (event.isControlDown()) {
                final var factor = event.getDeltaY() > 0 ? 1.1 : 0.9;
                final var newScaleX = Math.max(0.2, Math.min(3.0, canvas.getScaleX() * factor));
                final var newScaleY = Math.max(0.2, Math.min(3.0, canvas.getScaleY() * factor));
                canvas.setScaleX(newScaleX);
                canvas.setScaleY(newScaleY);
                event.consume();
            }
        });

        showPlaceholder();
    }

    private void showPlaceholder() {
        final var placeholder = new Label("Open an XML file to view its schema");
        placeholder.setStyle("-fx-text-fill: #888; -fx-font-size: 16;");
        placeholder.setLayoutX(50);
        placeholder.setLayoutY(50);
        canvas.getChildren().add(placeholder);
    }

    public void render(ParseResult result) {
        canvas.getChildren().clear();
        canvas.setScaleX(1.0);
        canvas.setScaleY(1.0);
        nodeBoxes.clear();
        nodePositions.clear();

        final var schemaMap = result.getSchemaMap();
        if (schemaMap.isEmpty()) {
            showPlaceholder();
            return;
        }

        final var levels = assignLevels(schemaMap);

        final Map<Integer, List<String>> byLevel = new TreeMap<>();
        for (var entry : levels.entrySet()) {
            byLevel.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }

        computePositions(schemaMap, byLevel);
        drawEdges(schemaMap, levels);

        for (var entry : schemaMap.entrySet()) {
            final var tagName = entry.getKey();
            if (!nodePositions.containsKey(tagName)) continue;
            final var box = createEntityBox(entry.getValue());
            final var pos = nodePositions.get(tagName);
            box.setLayoutX(pos[0] - pos[2] / 2);
            box.setLayoutY(pos[1] - pos[3] / 2);
            canvas.getChildren().add(box);
            nodeBoxes.put(tagName, box);
        }
    }

    private Map<String, Integer> assignLevels(Map<String, SchemaNode> schemaMap) {
        final Set<String> allChildTypes = new HashSet<>();
        for (var node : schemaMap.values()) {
            allChildTypes.addAll(node.getChildren().keySet());
        }

        final List<String> roots = new ArrayList<>();
        for (var tag : schemaMap.keySet()) {
            if (!allChildTypes.contains(tag)) {
                roots.add(tag);
            }
        }
        if (roots.isEmpty() && !schemaMap.isEmpty()) {
            roots.add(schemaMap.keySet().iterator().next());
        }

        final Map<String, Integer> levels = new LinkedHashMap<>();
        final Queue<String> queue = new LinkedList<>();
        for (final var root : roots) {
            levels.put(root, 0);
            queue.add(root);
        }
        while (!queue.isEmpty()) {
            final var current = queue.poll();
            final var level = levels.get(current);
            final var node = schemaMap.get(current);
            if (node == null) continue;
            for (final var child : node.getChildren().keySet()) {
                if (!levels.containsKey(child)) {
                    levels.put(child, level + 1);
                    queue.add(child);
                }
            }
        }

        for (final var tag : schemaMap.keySet()) {
            levels.putIfAbsent(tag, 0);
        }

        return levels;
    }

    private void computePositions(Map<String, SchemaNode> schemaMap,
                                  Map<Integer, List<String>> byLevel) {
        double y = PADDING;

        for (var levelEntry : byLevel.entrySet()) {
            final var nodesAtLevel = levelEntry.getValue();
            double maxHeight = 0;
            final List<double[]> sizes = new ArrayList<>();

            for (final var tag : nodesAtLevel) {
                final var node = schemaMap.get(tag);
                final var h = estimateBoxHeight(node);
                sizes.add(new double[]{NODE_WIDTH, h});
                maxHeight = Math.max(maxHeight, h);
            }

            double x = PADDING;
            for (int i = 0; i < nodesAtLevel.size(); i++) {
                final var tag = nodesAtLevel.get(i);
                final var w = sizes.get(i)[0];
                final var h = sizes.get(i)[1];
                final var centerX = x + w / 2;
                final var centerY = y + maxHeight / 2;
                nodePositions.put(tag, new double[]{centerX, centerY, w, h});
                x += w + H_GAP;
            }

            y += maxHeight + V_GAP;
        }

        double maxX = 0, maxY = 0;
        for (var pos : nodePositions.values()) {
            maxX = Math.max(maxX, pos[0] + pos[2] / 2 + PADDING);
            maxY = Math.max(maxY, pos[1] + pos[3] / 2 + PADDING);
        }
        canvas.setMinWidth(maxX);
        canvas.setMinHeight(maxY);
        canvas.setPrefWidth(maxX);
        canvas.setPrefHeight(maxY);
    }

    private double estimateBoxHeight(SchemaNode node) {
        int lines = node.getAttributes().size();
        if (node.isHasTextContent()) lines++;
        return 50 + Math.max(lines, 1) * 22 + 10;
    }

    private Region createEntityBox(SchemaNode node) {
        final var box = new VBox();
        box.setPrefWidth(NODE_WIDTH);
        box.setMaxWidth(NODE_WIDTH);
        box.setStyle("-fx-background-color: white; -fx-border-color: #4a86c8; "
                + "-fx-border-width: 2; -fx-border-radius: 4; -fx-background-radius: 4; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 4, 0, 1, 1);");

        final var header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 10, 6, 10));
        header.setStyle("-fx-background-color: #4a86c8; -fx-background-radius: 2 2 0 0;");

        final var tagLabel = new Label(node.getTagName());
        tagLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        tagLabel.setTextFill(Color.WHITE);
        HBox.setHgrow(tagLabel, Priority.ALWAYS);

        final var countLabel = new Label("(" + formatCount(node.getInstanceCount()) + ")");
        countLabel.setTextFill(Color.web("#d4e4f7"));
        countLabel.setFont(Font.font("System", 11));

        header.getChildren().addAll(tagLabel, countLabel);
        box.getChildren().add(header);
        box.getChildren().add(new Separator());

        final var attrBox = new VBox(2);
        attrBox.setPadding(new Insets(4, 10, 6, 10));

        for (var attrEntry : node.getAttributes().entrySet()) {
            final var attr = attrEntry.getValue();
            final var attrLabel = new Label(attr.getName() + " : " + attr.getInferredType());
            attrLabel.setFont(Font.font("Monospaced", 11));
            attrLabel.setTextFill(Color.web("#333"));
            attrBox.getChildren().add(attrLabel);
        }

        if (node.isHasTextContent()) {
            final var textLabel = new Label("[text content]");
            textLabel.setFont(Font.font("Monospaced", 11));
            textLabel.setTextFill(Color.web("#888"));
            textLabel.setStyle("-fx-font-style: italic;");
            attrBox.getChildren().add(textLabel);
        }

        if (attrBox.getChildren().isEmpty()) {
            final var emptyLabel = new Label("(no attributes)");
            emptyLabel.setFont(Font.font("System", 11));
            emptyLabel.setTextFill(Color.web("#aaa"));
            attrBox.getChildren().add(emptyLabel);
        }

        box.getChildren().add(attrBox);
        return box;
    }

    private void drawEdges(Map<String, SchemaNode> schemaMap, Map<String, Integer> levels) {
        for (var entry : schemaMap.entrySet()) {
            final var parentTag = entry.getKey();
            final var parentNode = entry.getValue();
            final var parentPos = nodePositions.get(parentTag);
            if (parentPos == null) continue;

            for (var childEntry : parentNode.getChildren().entrySet()) {
                final var childTag = childEntry.getKey();
                final var childInfo = childEntry.getValue();
                final var childPos = nodePositions.get(childTag);
                if (childPos == null) continue;

                if (parentTag.equals(childTag)) {
                    drawSelfEdge(parentPos, childInfo.getCardinality());
                    continue;
                }

                drawEdge(parentPos, childPos, childInfo.getCardinality());
            }
        }
    }

    private void drawEdge(double[] from, double[] to, String cardinality) {
        final var startX = from[0];
        final var startY = from[1] + from[3] / 2;
        final var endX = to[0];
        final var endY = to[1] - to[3] / 2;

        final var line = new Line(startX, startY, endX, endY);
        line.setStroke(Color.web("#666"));
        line.setStrokeWidth(1.5);
        canvas.getChildren().add(line);

        drawArrowhead(startX, startY, endX, endY);

        final var labelX = endX + 8;
        final var labelY = endY - 12;
        final var label = new Label(cardinality);
        label.setFont(Font.font("System", FontWeight.BOLD, 10));
        label.setTextFill(Color.web("#c0392b"));
        label.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-padding: 1 3;");
        label.setLayoutX(labelX);
        label.setLayoutY(labelY);
        canvas.getChildren().add(label);
    }

    private void drawArrowhead(double startX, double startY, double endX, double endY) {
        final var angle = Math.atan2(endY - startY, endX - startX);

        final var x1 = endX - 10 * Math.cos(angle - Math.PI / 6);
        final var y1 = endY - 10 * Math.sin(angle - Math.PI / 6);
        final var x2 = endX - 10 * Math.cos(angle + Math.PI / 6);
        final var y2 = endY - 10 * Math.sin(angle + Math.PI / 6);

        final var arrowhead = new Polygon(endX, endY, x1, y1, x2, y2);
        arrowhead.setFill(Color.web("#666"));
        canvas.getChildren().add(arrowhead);
    }

    private void drawSelfEdge(double[] pos, String cardinality) {
        final var x = pos[0] + pos[2] / 2;
        final var y = pos[1];
        final var loopSize = 30.0;

        final var l1 = new Line(x, y - 10, x + loopSize, y - 10);
        final var l2 = new Line(x + loopSize, y - 10, x + loopSize, y + 10);
        final var l3 = new Line(x + loopSize, y + 10, x, y + 10);
        for (var l : List.of(l1, l2, l3)) {
            l.setStroke(Color.web("#666"));
            l.setStrokeWidth(1.5);
        }
        canvas.getChildren().addAll(l1, l2, l3);

        drawArrowhead(x + loopSize, y + 10, x, y + 10);

        final var label = new Label(cardinality);
        label.setFont(Font.font("System", FontWeight.BOLD, 10));
        label.setTextFill(Color.web("#c0392b"));
        label.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-padding: 1 3;");
        label.setLayoutX(x + loopSize + 4);
        label.setLayoutY(y - 8);
        canvas.getChildren().add(label);
    }

    private String formatCount(int count) {
        if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000.0);
        if (count >= 1_000) return String.format("%.1fK", count / 1_000.0);
        return String.valueOf(count);
    }
}
