package com.kdue.browserfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class BrowserApplication extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserApplication.class);
    private static final String FXML_RESOURCE = "browser-view.fxml";
    private static final double WINDOW_WIDTH = 1024;
    private static final double WINDOW_HEIGHT = 768;
    private static final String WINDOW_TITLE = "BrowserFX";

    @Override
    public void start(Stage stage) {
        Objects.requireNonNull(stage, "Stage cannot be null");

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                Objects.requireNonNull(
                    BrowserApplication.class.getResource(FXML_RESOURCE),
                    "FXML resource not found: " + FXML_RESOURCE
                )
            );

            Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);

            stage.setTitle(WINDOW_TITLE);
            stage.setScene(scene);
            stage.setMinWidth(400);
            stage.setMinHeight(300);
            stage.show();

            LOGGER.info("BrowserFX started successfully");

        } catch (IOException e) {
            LOGGER.error("Failed to load FXML resource", e);
            showErrorDialog("Failed to load application view", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error during application startup", e);
            showErrorDialog("Unexpected error occurred", e);
        }
    }

    private void showErrorDialog(String message, Exception e) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}
