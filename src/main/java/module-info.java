module dev.isira.xmlviz {
    requires javafx.controls;
    requires java.xml;
    requires org.jspecify;

    opens dev.isira.xmlviz to javafx.graphics;
    opens dev.isira.xmlviz.model to javafx.base;
    opens dev.isira.xmlviz.ui to javafx.graphics;

    exports dev.isira.xmlviz;
    exports dev.isira.xmlviz.model;
    exports dev.isira.xmlviz.parsing;
    exports dev.isira.xmlviz.ui;
}
