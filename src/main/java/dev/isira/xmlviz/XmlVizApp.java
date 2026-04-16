package dev.isira.xmlviz;

import dev.isira.xmlviz.model.ParseResult;
import dev.isira.xmlviz.parsing.XmlParser;
import dev.isira.xmlviz.ui.ErdView;
import dev.isira.xmlviz.ui.InstanceTreeView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class XmlVizApp extends Application {

    private ErdView erdView;
    private InstanceTreeView instanceTreeView;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Label elementCountLabel;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        BorderPane root = new BorderPane();

        // Menu bar
        root.setTop(createMenuBar());

        // Tab pane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        erdView = new ErdView();
        Tab erdTab = new Tab("Schema / ERD", erdView);

        instanceTreeView = new InstanceTreeView();
        Tab treeTab = new Tab("Instance Tree", instanceTreeView);

        tabPane.getTabs().addAll(erdTab, treeTab);
        root.setCenter(tabPane);

        // Status bar
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root, 1200, 800);

        // Drag-and-drop XML files onto the window
        scene.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        scene.setOnDragDropped(event -> {
            var files = event.getDragboard().getFiles();
            if (files != null && !files.isEmpty()) {
                File file = files.getFirst();
                if (file.getName().toLowerCase().endsWith(".xml")) {
                    parseFile(file);
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });

        stage.setTitle("XML Structure Visualizer");
        stage.setScene(scene);
        stage.show();

        // If a file was passed as argument, open it
        var params = getParameters().getRaw();
        if (!params.isEmpty()) {
            File argFile = new File(params.getFirst());
            if (argFile.isFile()) {
                parseFile(argFile);
            }
        }
    }

    private MenuBar createMenuBar() {
        // File menu
        MenuItem openItem = new MenuItem("Open...");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        openItem.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Open XML File");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("XML Files", "*.xml", "*.xsd", "*.xsl", "*.xslt", "*.svg", "*.xhtml"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            File file = fc.showOpenDialog(primaryStage);
            if (file != null) {
                parseFile(file);
            }
        });

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());

        Menu fileMenu = new Menu("File", null, openItem, new SeparatorMenuItem(), exitItem);

        return new MenuBar(fileMenu);
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 10, 4, 10));
        bar.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label("Ready — open an XML file or drag & drop");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setVisible(false);

        elementCountLabel = new Label();

        bar.getChildren().addAll(statusLabel, progressBar, elementCountLabel);
        return bar;
    }

    private void parseFile(File file) {
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        statusLabel.setText("Parsing: " + file.getName() + " (" + formatFileSize(file.length()) + ")");
        elementCountLabel.setText("");

        Task<ParseResult> task = new Task<>() {
            @Override
            protected ParseResult call() throws Exception {
                XmlParser parser = new XmlParser();
                return parser.parse(file, progress ->
                        Platform.runLater(() -> progressBar.setProgress(progress)));
            }
        };

        task.setOnSucceeded(e -> {
            ParseResult result = task.getValue();
            progressBar.setVisible(false);
            statusLabel.setText("File: " + file.getName());
            elementCountLabel.setText(String.format("%,d elements | %d types",
                    result.getTotalElements(), result.getSchemaMap().size()));

            erdView.render(result);
            instanceTreeView.render(result);

            primaryStage.setTitle("XML Structure Visualizer — " + file.getName());
        });

        task.setOnFailed(e -> {
            progressBar.setVisible(false);
            Throwable ex = task.getException();
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            statusLabel.setText("Error: " + msg);
            elementCountLabel.setText("");

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Parse Error");
            alert.setHeaderText("Failed to parse " + file.getName());
            alert.setContentText(msg);
            alert.showAndWait();
        });

        Thread thread = new Thread(task, "xml-parser");
        thread.setDaemon(true);
        thread.start();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
