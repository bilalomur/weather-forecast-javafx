package org.example.project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the FXML file from the resources folder relative to the package
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/project/hello-view.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Weather Forecast Exam App");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}