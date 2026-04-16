package dev.isira.xmlviz.ui;

import dev.isira.xmlviz.model.IndexEntry;
import dev.isira.xmlviz.model.ParseResult;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;

public class InstanceTreeView extends SplitPane {

    private final TreeView<IndexEntry> treeView = new TreeView<>();
    private final VBox detailPanel = new VBox(8);
    private final Label tagNameLabel = new Label();
    private final Label xpathLabel = new Label();
    private final Label depthLabel = new Label();
    private final Label childCountLabel = new Label();
    private final TableView<Map.Entry<String, String>> attrTable = new TableView<>();
    private final TextArea textContentArea = new TextArea();
    private final HBox searchBar = new HBox(6);
    private final TextField searchField = new TextField();
    private final Label resultLabel = new Label();
    private final Set<Integer> searchResultSet = new HashSet<>();
    private ParseResult currentResult;
    private Task<List<Integer>> activeSearchTask;
    private List<Integer> searchResults = List.of();
    private int currentResultIndex = -1;

    public InstanceTreeView() {
        setOrientation(Orientation.HORIZONTAL);
        setDividerPositions(0.5);

        setupSearchBar();
        setupTreeView();
        setupDetailPanel();

        final var leftPane = new VBox();
        VBox.setVgrow(treeView, Priority.ALWAYS);
        leftPane.getChildren().addAll(searchBar, treeView);

        getItems().addAll(leftPane, new ScrollPane(detailPanel));

        showPlaceholder();
    }

    private void showPlaceholder() {
        final TreeItem<IndexEntry> placeholder = new TreeItem<>(null);
        treeView.setRoot(placeholder);
        treeView.setShowRoot(false);
    }

    private void setupTreeView() {
        treeView.setCellFactory(_ -> new TreeCell<>() {
            @Override
            protected void updateItem(IndexEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(entry.toString());
                    if (searchResultSet.contains(entry.getId())) {
                        if (currentResultIndex >= 0
                                && searchResults.get(currentResultIndex) == entry.getId()) {
                            setStyle("-fx-font-family: monospace; -fx-background-color: #ff9632;");
                        } else {
                            setStyle("-fx-font-family: monospace; -fx-background-color: #fff3cd;");
                        }
                    } else {
                        setStyle("-fx-font-family: monospace;");
                    }
                }
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((_, _, selected) -> {
            if (selected != null && selected.getValue() != null) {
                showDetail(selected.getValue());
            } else {
                clearDetail();
            }
        });

        treeView.setOnKeyPressed(event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
                toggleSearch();
                event.consume();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void setupDetailPanel() {
        detailPanel.setPadding(new Insets(12));
        detailPanel.setStyle("-fx-background-color: #fafafa;");

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
        clearSearch();
        final var roots = result.getRootEntries();

        if (roots.isEmpty()) {
            showPlaceholder();
            return;
        }

        if (roots.size() == 1) {
            final var rootItem = createTreeItem(roots.getFirst());
            treeView.setRoot(rootItem);
            rootItem.setExpanded(true);
        } else {
            final TreeItem<IndexEntry> syntheticRoot = new TreeItem<>(null);
            syntheticRoot.setExpanded(true);
            for (final var root : roots) {
                syntheticRoot.getChildren().add(createTreeItem(root));
            }
            treeView.setRoot(syntheticRoot);
            treeView.setShowRoot(false);
        }

        clearDetail();
    }

    private TreeItem<IndexEntry> createTreeItem(IndexEntry entry) {
        final TreeItem<IndexEntry> item = new TreeItem<>(entry);

        if (entry.getChildCount() > 0) {
            item.getChildren().add(new TreeItem<>(null));

            item.expandedProperty().addListener((_, _, isExpanded) -> {
                if (isExpanded && item.getChildren().size() == 1
                        && item.getChildren().getFirst().getValue() == null) {
                    loadChildren(item);
                }
            });
        }

        return item;
    }

    private void loadChildren(TreeItem<IndexEntry> parentItem) {
        final var parent = parentItem.getValue();
        parentItem.getChildren().clear();

        final var childIds = currentResult.getChildIds(parent.getId());
        for (final var childId : childIds) {
            final var childEntry = currentResult.getInstanceIndex().get(childId);
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

        final var text = entry.getTextPreview();
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
        final var label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        return label;
    }

    // --- Search functionality ---

    private void setupSearchBar() {
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(4, 8, 4, 8));
        searchBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");
        searchBar.setVisible(false);
        searchBar.setManaged(false);

        searchField.setPromptText("Search elements...");
        searchField.setPrefWidth(250);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    navigatePrevious();
                } else {
                    if (searchResults.isEmpty()
                            || !searchField.getText().strip().equalsIgnoreCase(currentSearchQuery())) {
                        executeSearch(searchField.getText());
                    } else {
                        navigateNext();
                    }
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                toggleSearch();
                event.consume();
            }
        });

        resultLabel.setStyle("-fx-text-fill: #666;");

        final var prevButton = new Button("▲");
        prevButton.setTooltip(new Tooltip("Previous match (Shift+Enter)"));
        prevButton.setOnAction(_ -> navigatePrevious());

        final var nextButton = new Button("▼");
        nextButton.setTooltip(new Tooltip("Next match (Enter)"));
        nextButton.setOnAction(_ -> navigateNext());

        final var closeButton = new Button("✕");
        closeButton.setTooltip(new Tooltip("Close search (Escape)"));
        closeButton.setOnAction(_ -> toggleSearch());

        searchBar.getChildren().addAll(searchField, resultLabel, prevButton, nextButton, closeButton);
    }

    private String currentSearchQuery() {
        return searchField.getText().strip().toLowerCase();
    }

    private void toggleSearch() {
        final var visible = !searchBar.isVisible();
        searchBar.setVisible(visible);
        searchBar.setManaged(visible);
        if (visible) {
            searchField.requestFocus();
            searchField.selectAll();
        } else {
            clearSearch();
            treeView.requestFocus();
        }
    }

    private void executeSearch(String query) {
        if (activeSearchTask != null && activeSearchTask.isRunning()) {
            activeSearchTask.cancel();
        }

        final var normalizedQuery = query.strip().toLowerCase();
        if (normalizedQuery.isEmpty() || currentResult == null) {
            clearSearch();
            return;
        }

        resultLabel.setText("Searching...");

        final var task = new Task<List<Integer>>() {
            @Override
            protected List<Integer> call() {
                final List<Integer> matches = new ArrayList<>(1024);
                final var index = currentResult.getInstanceIndex();
                for (int i = 0, size = index.size(); i < size; i++) {
                    if (isCancelled()) return List.of();
                    if (index.get(i).matches(normalizedQuery)) {
                        matches.add(i);
                        if (matches.size() >= 10_000) break;
                    }
                }
                return matches;
            }
        };

        task.setOnSucceeded(_ -> {
            searchResults = task.getValue();
            searchResultSet.clear();
            searchResultSet.addAll(searchResults);
            currentResultIndex = searchResults.isEmpty() ? -1 : 0;
            updateResultLabel();
            treeView.refresh();
            if (!searchResults.isEmpty()) {
                navigateToResult(0);
            }
        });

        task.setOnCancelled(_ -> {
            // no-op — a new search will replace this one
        });

        activeSearchTask = task;
        final var thread = new Thread(task, "tree-search");
        thread.setDaemon(true);
        thread.start();
    }

    private void clearSearch() {
        if (activeSearchTask != null && activeSearchTask.isRunning()) {
            activeSearchTask.cancel();
        }
        searchResults = List.of();
        searchResultSet.clear();
        currentResultIndex = -1;
        resultLabel.setText("");
        searchField.clear();
        treeView.refresh();
    }

    private void updateResultLabel() {
        if (searchResults.isEmpty()) {
            resultLabel.setText("No matches");
        } else if (searchResults.size() >= 10_000) {
            resultLabel.setText((currentResultIndex + 1) + " of 10,000+ matches");
        } else {
            resultLabel.setText((currentResultIndex + 1) + " of " + searchResults.size() + " matches");
        }
    }

    private void navigateNext() {
        if (searchResults.isEmpty()) return;
        navigateToResult((currentResultIndex + 1) % searchResults.size());
    }

    private void navigatePrevious() {
        if (searchResults.isEmpty()) return;
        navigateToResult((currentResultIndex - 1 + searchResults.size()) % searchResults.size());
    }

    private void navigateToResult(int resultIndex) {
        currentResultIndex = resultIndex;
        updateResultLabel();

        final var entryId = searchResults.get(resultIndex);
        final var chain = buildAncestorChain(entryId);

        var currentItem = treeView.getRoot();
        if (currentItem == null) return;

        // Determine starting index in chain based on root structure
        int startIndex = 0;
        if (currentItem.getValue() != null && currentItem.getValue().getId() == chain.getFirst()) {
            currentItem.setExpanded(true);
            startIndex = 1;
        }

        for (int i = startIndex; i < chain.size(); i++) {
            final var targetId = chain.get(i);
            currentItem.setExpanded(true);

            TreeItem<IndexEntry> found = null;
            for (final var child : currentItem.getChildren()) {
                if (child.getValue() != null && child.getValue().getId() == targetId) {
                    found = child;
                    break;
                }
            }
            if (found == null) break;
            currentItem = found;
        }

        final var targetRow = treeView.getRow(currentItem);
        treeView.getSelectionModel().select(targetRow);
        treeView.scrollTo(targetRow);
        treeView.refresh();
    }

    private List<Integer> buildAncestorChain(int entryId) {
        final List<Integer> chain = new ArrayList<>();
        var currentId = entryId;
        while (currentId >= 0) {
            chain.add(currentId);
            currentId = currentResult.getInstanceIndex().get(currentId).getParentId();
        }
        Collections.reverse(chain);
        return chain;
    }
}
