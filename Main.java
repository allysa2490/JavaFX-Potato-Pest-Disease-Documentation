import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.util.Properties;

public class Main extends Application {

    private static Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;

        Properties config = loadConfig();

        // Read config values
        String startPage = config.getProperty("start.page", "Home");
        String title = config.getProperty("title", "JavaFX App");
        int width = Integer.parseInt(config.getProperty("width", "1280"));
        int height = Integer.parseInt(config.getProperty("height", "720"));
        boolean maximized = Boolean.parseBoolean(config.getProperty("maximized", "false"));

        // Load first page
        go(startPage);

        // Window settings
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setMaximized(maximized);

        stage.setTitle(title);
        stage.show();
    }

    private Properties loadConfig() {
        Properties prop = new Properties();

        try (FileInputStream fis = new FileInputStream("config.properties")) {
            prop.load(fis);
        } catch (Exception e) {
            System.out.println("config.properties not found, using defaults.");
        }

        return prop;
    }

    public static void go(String pageName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource(pageName + ".fxml")
            );

            Parent root = loader.load();

            Scene scene = new Scene(root);

            // 👉 Add this line
            scene.getStylesheets().add(
                    Main.class.getResource("style.css").toExternalForm()
            );

            stage.setScene(scene);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load page: " + pageName, e);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}