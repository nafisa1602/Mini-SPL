package com.minispl;

import com.minispl.persistence.DatabaseManager;
import com.minispl.persistence.DatabaseSeeder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void init() {
        // Initialize SQLite schema and populate baseline seed data if empty
        DatabaseManager.getInstance().initializeDatabase();
        new DatabaseSeeder().seedIfEmpty();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/minispl/presentation/main-view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1220, 780);
        primaryStage.setTitle("DFIR Orchestrator | Desktop Incident Response & Forensics Engine");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
