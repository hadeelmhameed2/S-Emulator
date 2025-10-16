module hadeel.semulatorui {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.xml;

    opens hadeel.semulatorui to javafx.fxml;
    exports hadeel.semulatorui;
}