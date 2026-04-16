package dev.isira.xmlviz.ui;

import dev.isira.xmlviz.model.IndexEntry;
import dev.isira.xmlviz.model.ParseResult;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.*;

public class TreeSearchBar extends HBox {

    private final TreeView<IndexEntry> treeView;
    private final TextField searchField = new TextField();
    private final Label resultLabel = new Label();
    private final Set<Integer> searchResultSet = new HashSet<>();
    private ParseResult currentResult;
    private Task<List<Integer>> activeSearchTask;
    private List<Integer> searchResults = List.of();
    private int currentResultIndex = -1;
    private String lastSearchedQuery = "";

    public TreeSearchBar(TreeView<IndexEntry> treeView) {
        super(6);
        this.treeView = treeView;

        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(4, 8, 4, 8));
        setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");
        setVisible(false);
        setManaged(false);

        searchField.setPromptText("Search elements...");
        searchField.setPrefWidth(250);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    navigatePrevious();
                } else {
                    if (searchResults.isEmpty()
                            || !searchField.getText().strip().equalsIgnoreCase(lastSearchedQuery)) {
                        executeSearch(searchField.getText());
                    } else {
                        navigateNext();
                    }
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                toggle();
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
        closeButton.setOnAction(_ -> toggle());

        getChildren().addAll(searchField, resultLabel, prevButton, nextButton, closeButton);
    }

    public void setParseResult(ParseResult result) {
        this.currentResult = result;
    }

    public void toggle() {
        final var visible = !isVisible();
        setVisible(visible);
        setManaged(visible);
        if (visible) {
            searchField.requestFocus();
            searchField.selectAll();
        } else {
            clear();
            treeView.requestFocus();
        }
    }

    public void clear() {
        if (activeSearchTask != null && activeSearchTask.isRunning()) {
            activeSearchTask.cancel();
        }
        searchResults = List.of();
        searchResultSet.clear();
        currentResultIndex = -1;
        lastSearchedQuery = "";
        resultLabel.setText("");
        searchField.clear();
        treeView.refresh();
    }

    public boolean isMatch(int entryId) {
        return searchResultSet.contains(entryId);
    }

    public boolean isCurrentMatch(int entryId) {
        return currentResultIndex >= 0 && searchResults.get(currentResultIndex) == entryId;
    }

    private void executeSearch(String query) {
        if (activeSearchTask != null && activeSearchTask.isRunning()) {
            activeSearchTask.cancel();
        }

        final var normalizedQuery = query.strip().toLowerCase();
        if (normalizedQuery.isEmpty() || currentResult == null) {
            clear();
            return;
        }

        lastSearchedQuery = normalizedQuery;
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
