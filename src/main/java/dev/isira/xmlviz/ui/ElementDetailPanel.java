package dev.isira.xmlviz.ui;

import dev.isira.xmlviz.model.IndexEntry;
import dev.isira.xmlviz.model.ParseResult;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;

public class ElementDetailPanel extends VBox {

    private final Label tagNameLabel = new Label();
    private final Label xpathLabel = new Label();
    private final Label depthLabel = new Label();
    private final Label childCountLabel = new Label();
    private final TableView<Map.Entry<String, String>> attrTable = new TableView<>();
    private final TextArea textContentArea = new TextArea();

    @SuppressWarnings("unchecked")
    public ElementDetailPanel() {
        super(8);
        setPadding(new Insets(12));
        setStyle("-fx-background-color: #fafafa;");

        final var headerLabel = new Label("Element Details");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        final var infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(4);

        infoGrid.add(boldLabel("Tag:"), 0, 0);
        infoGrid.add(tagNameLabel, 1, 0);
        infoGrid.add(boldLabel("XPath:"), 0, 1);
        infoGrid.add(xpathLabel, 1, 1);
        infoGrid.add(boldLabel("Depth:"), 0, 2);
        infoGrid.add(depthLabel, 1, 2);
        infoGrid.add(boldLabel("Children:"), 0, 3);
        infoGrid.add(childCountLabel, 1, 3);

        xpathLabel.setWrapText(true);
        xpathLabel.setMaxWidth(400);
        xpathLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");

        final var attrHeader = new Label("Attributes");
        attrHeader.setFont(Font.font("System", FontWeight.BOLD, 12));

        final TableColumn<Map.Entry<String, String>, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getKey()));
        nameCol.setPrefWidth(120);

        final TableColumn<Map.Entry<String, String>, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getValue()));
        valueCol.setPrefWidth(250);

        attrTable.getColumns().setAll(nameCol, valueCol);
        attrTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        attrTable.setPrefHeight(150);
        attrTable.setPlaceholder(new Label("No attributes"));

        final var textHeader = new Label("Text Content");
        textHeader.setFont(Font.font("System", FontWeight.BOLD, 12));
        textContentArea.setEditable(false);
        textContentArea.setWrapText(true);
        textContentArea.setPrefHeight(120);
        textContentArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12;");

        getChildren().addAll(
                headerLabel,
                new Separator(),
                infoGrid,
                new Separator(),
                attrHeader,
                attrTable,
                new Separator(),
                textHeader,
                textContentArea
        );

        clearDetail();
    }

    public void showDetail(IndexEntry entry, ParseResult result) {
        tagNameLabel.setText(entry.getTagName());
        if (result != null) {
            xpathLabel.setText(result.buildXPath(entry));
        }
        depthLabel.setText(String.valueOf(entry.getDepth()));
        childCountLabel.setText(String.valueOf(entry.getChildCount()));

        attrTable.getItems().clear();
        attrTable.getItems().addAll(entry.getAttributes().entrySet());

        final var text = entry.getTextPreview();
        if (text != null && !text.isEmpty()) {
            textContentArea.setText(text + (entry.isHasMoreText() ? "\n\n[truncated...]" : ""));
        } else {
            textContentArea.setText("(no text content)");
        }
    }

    public void clearDetail() {
        tagNameLabel.setText("-");
        xpathLabel.setText("-");
        depthLabel.setText("-");
        childCountLabel.setText("-");
        attrTable.getItems().clear();
        textContentArea.clear();
    }

    private Label boldLabel(String text) {
        final var label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        return label;
    }
}
