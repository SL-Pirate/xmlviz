package dev.isira.xmlviz.ui;

import dev.isira.xmlviz.model.IndexEntry;
import dev.isira.xmlviz.model.ParseResult;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Map;

/**
 * Instance tree view with lazy-loading TreeView on the left
 * and a detail panel on the right.
 */
public class InstanceTreeView extends SplitPane {

    private final TreeView<IndexEntry> treeView = new TreeView<>();
    private final VBox detailPanel = new VBox(8);
    private ParseResult currentResult;

    // Detail panel labels
    private final Label tagNameLabel = new Label();
    private final Label xpathLabel = new Label();
    private final Label depthLabel = new Label();
    private final Label childCountLabel = new Label();
    private final TableView<Map.Entry<String, String>> attrTable = new TableView<>();
    private final TextArea textContentArea = new TextArea();

    public InstanceTreeView() {
        setOrientation(Orientation.HORIZONTAL);
        setDividerPositions(0.5);

        setupTreeView();
        setupDetailPanel();

        getItems().addAll(treeView, new ScrollPane(detailPanel));

        showPlaceholder();
    }

    private void showPlaceholder() {
        TreeItem<IndexEntry> placeholder = new TreeItem<>(null);
        treeView.setRoot(placeholder);
        treeView.setShowRoot(false);
    }

    private void setupTreeView() {
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(IndexEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(entry.toString());
                    setStyle("-fx-font-family: monospace;");
                }
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && selected.getValue() != null) {
                showDetail(selected.getValue());
            } else {
                clearDetail();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void setupDetailPanel() {
        detailPanel.setPadding(new Insets(12));
        detailPanel.setStyle("-fx-background-color: #fafafa;");

        Label headerLabel = new Label("Element Details");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        // Info fields
        GridPane infoGrid = new GridPane();
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

        // Attributes table
        Label attrHeader = new Label("Attributes");
        attrHeader.setFont(Font.font("System", FontWeight.BOLD, 12));

        TableColumn<Map.Entry<String, String>, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getKey()));
        nameCol.setPrefWidth(120);

        TableColumn<Map.Entry<String, String>, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getValue()));
        valueCol.setPrefWidth(250);

        attrTable.getColumns().setAll(nameCol, valueCol);
        attrTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        attrTable.setPrefHeight(150);
        attrTable.setPlaceholder(new Label("No attributes"));

        // Text content
        Label textHeader = new Label("Text Content");
        textHeader.setFont(Font.font("System", FontWeight.BOLD, 12));
        textContentArea.setEditable(false);
        textContentArea.setWrapText(true);
        textContentArea.setPrefHeight(120);
        textContentArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12;");

        detailPanel.getChildren().addAll(
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

    public void render(ParseResult result) {
        this.currentResult = result;
        List<IndexEntry> roots = result.getRootEntries();

        if (roots.isEmpty()) {
            showPlaceholder();
            return;
        }

        if (roots.size() == 1) {
            // Single root — make it the tree root
            TreeItem<IndexEntry> rootItem = createTreeItem(roots.get(0));
            treeView.setRoot(rootItem);
            rootItem.setExpanded(true);
        } else {
            // Multiple roots — synthetic root
            TreeItem<IndexEntry> syntheticRoot = new TreeItem<>(null);
            syntheticRoot.setExpanded(true);
            for (IndexEntry root : roots) {
                syntheticRoot.getChildren().add(createTreeItem(root));
            }
            treeView.setRoot(syntheticRoot);
            treeView.setShowRoot(false);
        }

        clearDetail();
    }

    private TreeItem<IndexEntry> createTreeItem(IndexEntry entry) {
        TreeItem<IndexEntry> item = new TreeItem<>(entry);

        if (entry.getChildCount() > 0) {
            // Add a dummy child to show the expand arrow
            item.getChildren().add(new TreeItem<>(null));

            item.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                if (isExpanded && item.getChildren().size() == 1
                        && item.getChildren().getFirst().getValue() == null) {
                    loadChildren(item);
                }
            });
        }

        return item;
    }

    private void loadChildren(TreeItem<IndexEntry> parentItem) {
        IndexEntry parent = parentItem.getValue();
        parentItem.getChildren().clear();

        List<Integer> childIds = currentResult.getChildIds(parent.getId());
        for (int childId : childIds) {
            IndexEntry childEntry = currentResult.getInstanceIndex().get(childId);
            parentItem.getChildren().add(createTreeItem(childEntry));
        }
    }

    private void showDetail(IndexEntry entry) {
        tagNameLabel.setText(entry.getTagName());
        if (currentResult != null) {
            xpathLabel.setText(currentResult.buildXPath(entry));
        }
        depthLabel.setText(String.valueOf(entry.getDepth()));
        childCountLabel.setText(String.valueOf(entry.getChildCount()));

        attrTable.getItems().clear();
        attrTable.getItems().addAll(entry.getAttributes().entrySet());

        String text = entry.getTextPreview();
        if (text != null && !text.isEmpty()) {
            textContentArea.setText(text + (entry.isHasMoreText() ? "\n\n[truncated...]" : ""));
        } else {
            textContentArea.setText("(no text content)");
        }
    }

    private void clearDetail() {
        tagNameLabel.setText("-");
        xpathLabel.setText("-");
        depthLabel.setText("-");
        childCountLabel.setText("-");
        attrTable.getItems().clear();
        textContentArea.clear();
    }

    private Label boldLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        return label;
    }
}
