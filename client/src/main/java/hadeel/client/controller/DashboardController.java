package hadeel.client.controller;

import hadeel.client.SEmulatorClientApp;
import hadeel.client.service.HttpClientService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DashboardController {

    @FXML private Label currentUserLabel;
    @FXML private Label creditsLabel;

    // Users table
    @FXML private TableView<Map<String, Object>> usersTable;
    @FXML private TableColumn<Map<String, Object>, String> colUserName;
    @FXML private TableColumn<Map<String, Object>, Integer> colUserPrograms;
    @FXML private TableColumn<Map<String, Object>, Integer> colUserFunctions;
    @FXML private TableColumn<Map<String, Object>, Integer> colUserCredits;
    @FXML private TableColumn<Map<String, Object>, Integer> colUserCreditsUsed;
    @FXML private TableColumn<Map<String, Object>, Integer> colUserExecutions;

    // Programs table
    @FXML private TableView<Map<String, Object>> programsTable;
    @FXML private TableColumn<Map<String, Object>, String> colProgramName;
    @FXML private TableColumn<Map<String, Object>, String> colProgramOwner;
    @FXML private TableColumn<Map<String, Object>, Integer> colProgramInstructions;
    @FXML private TableColumn<Map<String, Object>, Integer> colProgramMaxDegree;
    @FXML private TableColumn<Map<String, Object>, Integer> colProgramExecutions;
    @FXML private TableColumn<Map<String, Object>, Double> colProgramAvgCredits;

    // Functions table
    @FXML private TableView<Map<String, Object>> functionsTable;
    @FXML private TableColumn<Map<String, Object>, String> colFunctionName;
    @FXML private TableColumn<Map<String, Object>, String> colFunctionParent;
    @FXML private TableColumn<Map<String, Object>, String> colFunctionOwner;
    @FXML private TableColumn<Map<String, Object>, Integer> colFunctionInstructions;
    @FXML private TableColumn<Map<String, Object>, Integer> colFunctionMaxDegree;

    // Execution history table
    @FXML private TableView<Map<String, Object>> historyTable;
    @FXML private TableColumn<Map<String, Object>, Integer> colHistoryRunNumber;
    @FXML private TableColumn<Map<String, Object>, String> colHistoryProgram;
    @FXML private TableColumn<Map<String, Object>, Boolean> colHistoryIsFunction;
    @FXML private TableColumn<Map<String, Object>, String> colHistoryArchitecture;
    @FXML private TableColumn<Map<String, Object>, Integer> colHistoryDegree;
    @FXML private TableColumn<Map<String, Object>, Integer> colHistoryOutput;
    @FXML private TableColumn<Map<String, Object>, Integer> colHistoryCycles;
    @FXML private TableColumn<Map<String, Object>, Integer> colHistoryCredits;

    @FXML private Button uploadProgramBtn;
    @FXML private Button addCreditsBtn;
    @FXML private Button runProgramBtn;
    @FXML private Button logoutBtn;

    private HttpClientService httpClient;
    private ScheduledExecutorService updateService;
    private String currentUser;
    private String selectedUserForHistory;
    private ObservableList<Map<String, Object>> usersData = FXCollections.observableArrayList();
    private ObservableList<Map<String, Object>> programsData = FXCollections.observableArrayList();
    private ObservableList<Map<String, Object>> functionsData = FXCollections.observableArrayList();
    private ObservableList<Map<String, Object>> historyData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        httpClient = SEmulatorClientApp.getHttpClient();
        currentUser = SEmulatorClientApp.getCurrentUser();

        currentUserLabel.setText("User: " + currentUser);
        selectedUserForHistory = currentUser; // Default to current user

        setupTables();
        startRealTimeUpdates();
    }

    private void setupTables() {
        // Setup Users table
        colUserName.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("username")));
        colUserPrograms.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("programCount")).intValue()).asObject());
        colUserFunctions.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("functionCount")).intValue()).asObject());
        colUserCredits.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("credits")).intValue()).asObject());
        colUserCreditsUsed.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("creditsUsed")).intValue()).asObject());
        colUserExecutions.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("executionCount")).intValue()).asObject());
        usersTable.setItems(usersData);

        // User selection listener - show selected user's history
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedUserForHistory = (String) newSelection.get("username");
                refreshHistory();
            }
        });

        // Setup Programs table
        colProgramName.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("name")));
        colProgramOwner.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("owner")));
        colProgramInstructions.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("instructionCount")).intValue()).asObject());
        colProgramMaxDegree.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("maxDegree")).intValue()).asObject());
        colProgramExecutions.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("executionCount")).intValue()).asObject());
        colProgramAvgCredits.setCellValueFactory(data ->
            new javafx.beans.property.SimpleDoubleProperty(((Number) data.getValue().get("averageCredits")).doubleValue()).asObject());
        programsTable.setItems(programsData);

        // Setup Functions table
        colFunctionName.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("name")));
        colFunctionParent.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("parentProgram")));
        colFunctionOwner.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("owner")));
        colFunctionInstructions.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("instructionCount")).intValue()).asObject());
        colFunctionMaxDegree.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("maxDegree")).intValue()).asObject());
        functionsTable.setItems(functionsData);

        // Setup History table
        colHistoryRunNumber.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("runNumber")).intValue()).asObject());
        colHistoryProgram.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("programName")));
        colHistoryIsFunction.setCellValueFactory(data ->
            new javafx.beans.property.SimpleBooleanProperty((Boolean) data.getValue().get("isFunction")).asObject());
        colHistoryArchitecture.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("architecture")));
        colHistoryDegree.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("degree")).intValue()).asObject());
        colHistoryOutput.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("output")).intValue()).asObject());
        colHistoryCycles.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("cycles")).intValue()).asObject());
        colHistoryCredits.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(((Number) data.getValue().get("creditsUsed")).intValue()).asObject());
        historyTable.setItems(historyData);
    }

    private void startRealTimeUpdates() {
        updateService = Executors.newSingleThreadScheduledExecutor();
        updateService.scheduleAtFixedRate(() -> {
            Platform.runLater(this::refreshAllData);
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void refreshAllData() {
        refreshUsers();
        refreshPrograms();
        refreshFunctions();
        refreshHistory();
        updateCurrentUserCredits();
    }

    private void refreshUsers() {
        HttpClientService.Response<List<Map<String, Object>>> response = httpClient.getUsers();
        if (response.isSuccess()) {
            usersData.setAll(response.getData());
        }
    }

    private void refreshPrograms() {
        HttpClientService.Response<List<Map<String, Object>>> response = httpClient.getPrograms();
        if (response.isSuccess()) {
            programsData.setAll(response.getData());
        }
    }

    private void refreshFunctions() {
        HttpClientService.Response<List<Map<String, Object>>> response = httpClient.getFunctions();
        if (response.isSuccess()) {
            functionsData.setAll(response.getData());
        }
    }

    private void refreshHistory() {
        HttpClientService.Response<List<Map<String, Object>>> response =
            httpClient.getExecutionHistory(selectedUserForHistory);
        if (response.isSuccess()) {
            historyData.setAll(response.getData());
        }
    }

    private void updateCurrentUserCredits() {
        for (Map<String, Object> user : usersData) {
            if (currentUser.equals(user.get("username"))) {
                creditsLabel.setText("Credits: " + user.get("credits"));
                break;
            }
        }
    }

    @FXML
    private void handleUploadProgram() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select S-Program XML File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );

        File file = fileChooser.showOpenDialog(uploadProgramBtn.getScene().getWindow());
        if (file != null) {
            try {
                String xmlContent = Files.readString(file.toPath());
                HttpClientService.Response<Map<String, Object>> response =
                    httpClient.uploadProgram(currentUser, xmlContent);

                if (response.isSuccess()) {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Program uploaded successfully");
                    refreshAllData();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Upload Failed", response.getError());
                }
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "File Error",
                    "Error reading file: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddCredits() {
        TextInputDialog dialog = new TextInputDialog("1000");
        dialog.setTitle("Add Credits");
        dialog.setHeaderText("Add Credits to Your Account");
        dialog.setContentText("Amount:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amountStr -> {
            try {
                int amount = Integer.parseInt(amountStr);
                if (amount <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Amount",
                        "Amount must be positive");
                    return;
                }

                HttpClientService.Response<Map<String, Object>> response =
                    httpClient.addCredits(currentUser, amount);

                if (response.isSuccess()) {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Credits added successfully");
                    refreshAllData();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Failed", response.getError());
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input",
                    "Please enter a valid number");
            }
        });
    }

    @FXML
    private void handleRunProgram() {
        Map<String, Object> selectedProgram = programsTable.getSelectionModel().getSelectedItem();
        if (selectedProgram == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                "Please select a program to run");
            return;
        }

        String programName = (String) selectedProgram.get("name");
        openExecutionScreen(programName, false);
    }

    @FXML
    private void handleLogout() {
        if (updateService != null) {
            updateService.shutdown();
        }

        httpClient.logout(currentUser);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/hadeel/client/login.fxml"));
            Scene scene = new Scene(loader.load(), 400, 300);

            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            stage.setTitle("S-Emulator Client - Login");
            stage.setScene(scene);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                "Error loading login screen: " + e.getMessage());
        }
    }

    private void openExecutionScreen(String programName, boolean isFunction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/hadeel/client/execution.fxml"));
            Scene scene = new Scene(loader.load(), 1400, 900);

            ProgramExecutionController controller = loader.getController();
            controller.setProgramContext(programName, isFunction);

            Stage stage = new Stage();
            stage.setTitle("S-Emulator - Execute: " + programName);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                "Error loading execution screen: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void shutdown() {
        if (updateService != null) {
            updateService.shutdown();
        }
    }
}
