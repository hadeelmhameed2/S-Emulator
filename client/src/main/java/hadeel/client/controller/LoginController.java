package hadeel.client.controller;

import hadeel.client.SEmulatorClientApp;
import hadeel.client.service.HttpClientService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.Map;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            showError("Please enter a username");
            return;
        }

        HttpClientService httpClient = SEmulatorClientApp.getHttpClient();
        HttpClientService.Response<Map<String, Object>> response = httpClient.login(username);

        if (response.isSuccess()) {
            SEmulatorClientApp.setCurrentUser(username);

            // Load dashboard
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/hadeel/client/dashboard.fxml"));
                Scene scene = new Scene(loader.load(), 1200, 800);

                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setTitle("S-Emulator Client - Dashboard");
                stage.setScene(scene);
            } catch (Exception e) {
                showError("Error loading dashboard: " + e.getMessage());
            }
        } else {
            showError(response.getError());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
