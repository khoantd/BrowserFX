module com.kdue.browserfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires org.slf4j;
    requires okhttp3;

    opens com.kdue.browserfx to javafx.fxml;
    exports com.kdue.browserfx;
}
