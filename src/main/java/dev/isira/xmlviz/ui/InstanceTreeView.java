package dev.isira.xmlviz.ui;

import dev.isira.xmlviz.model.IndexEntry;
import dev.isira.xmlviz.model.ParseResult;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class InstanceTreeView extends SplitPane {

    private final TreeView<IndexEntry> treeView = new TreeView<>();
    private final ElementDetailPanel detailPanel = new ElementDetailPanel();
    private final TreeSearchBar searchBar;
    private ParseResult currentResult;

    public InstanceTreeView() {
        setOrientation(Orientation.HORIZONTAL);
        setDividerPositions(0.5);

        searchBar = new TreeSearchBar(treeView);
        setupTreeView();

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
                    if (searchBar.isMatch(entry.getId())) {
                        if (searchBar.isCurrentMatch(entry.getId())) {
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
                detailPanel.showDetail(selected.getValue(), currentResult);
            } else {
                detailPanel.clearDetail();
            }
        });

        treeView.setOnKeyPressed(event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
                searchBar.toggle();
                event.consume();
            }
        });
    }

    public void render(ParseResult result) {
        this.currentResult = result;
        searchBar.setParseResult(result);
        searchBar.clear();
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

        detailPanel.clearDetail();
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
}
