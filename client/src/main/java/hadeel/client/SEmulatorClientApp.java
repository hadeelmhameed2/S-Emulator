package hadeel.client;

import hadeel.client.controller.LoginController;
import hadeel.client.service.HttpClientService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SEmulatorClientApp extends Application {
    private static final String SERVER_URL = "http://localhost:8080/SEmulator";
    private static HttpClientService httpClient;
    private static String currentUser;

    @Override
    public void start(Stage primaryStage) throws Exception {
        httpClient = new HttpClientService(SERVER_URL);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
        Scene scene = new Scene(loader.load(), 400, 300);

        primaryStage.setTitle("S-Emulator Client - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static HttpClientService getHttpClient() {
        return httpClient;
    }

    public static String getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(String username) {
        currentUser = username;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
