module dev.isira.xmlviz {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;

    opens dev.isira.xmlviz to javafx.fxml, javafx.graphics;
    opens dev.isira.xmlviz.model to javafx.base;
    opens dev.isira.xmlviz.ui to javafx.fxml, javafx.graphics;

    exports dev.isira.xmlviz;
    exports dev.isira.xmlviz.model;
    exports dev.isira.xmlviz.parsing;
    exports dev.isira.xmlviz.ui;
}
