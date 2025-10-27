package hadeel.client.controller;

import hadeel.client.SEmulatorClientApp;
import hadeel.client.service.HttpClientService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProgramExecutionController {

    @FXML private Label programNameLabel;
    @FXML private Label creditsLabel;
    @FXML private Label degreeLabel;
    @FXML private Label architectureSummaryLabel;

    @FXML private ComboBox<String> architectureSelector;
    @FXML private Spinner<Integer> degreeSpinner;
    @FXML private TextArea inputsArea;
    @FXML private Button expandBtn;
    @FXML private Button collapseBtn;

    @FXML private TableView<InstructionRow> instructionsTable;
    @FXML private TableColumn<InstructionRow, Boolean> colBreakpoint;
    @FXML private TableColumn<InstructionRow, Integer> colNumber;
    @FXML private TableColumn<InstructionRow, String> colType;
    @FXML private TableColumn<InstructionRow, String> colArchitecture;
    @FXML private TableColumn<InstructionRow, String> colLabel;
    @FXML private TableColumn<InstructionRow, String> colInstruction;
    @FXML private TableColumn<InstructionRow, Integer> colCycles;

    @FXML private Button runBtn;
    @FXML private Button debugBtn;
    @FXML private Button stepOverBtn;
    @FXML private Button stepBackBtn;
    @FXML private Button resumeBtn;
    @FXML private Button stopBtn;
    @FXML private Button backToDashboardBtn;

    @FXML private TextArea variablesArea;
    @FXML private Label cyclesLabelBottom;

    private HttpClientService httpClient;
    private String currentUser;
    private String programName;
    private boolean isFunction;
    private int currentDegree = 0;
    private String selectedArchitecture = "I";
    private String debugSessionId;
    private boolean isDebugging = false;
    private ScheduledExecutorService creditUpdateService;
    private Set<Integer> breakpoints = new HashSet<>();
    private ObservableList<InstructionRow> instructionRows = FXCollections.observableArrayList();

    public void setProgramContext(String programName, boolean isFunction) {
        this.programName = programName;
        this.isFunction = isFunction;
        programNameLabel.setText("Program: " + programName);

        // Load and display program instructions
        loadProgramInstructions();
    }

    private void loadProgramInstructions() {
        HttpClientService.Response<Map<String, Object>> response = httpClient.getProgramDetails(programName);

        if (!response.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load program: " + response.getError());
            return;
        }

        Map<String, Object> programData = response.getData();

        // Update max degree label
        int maxDegree = ((Number) programData.get("maxDegree")).intValue();
        degreeLabel.setText("Degree: " + currentDegree + " / " + maxDegree);

        // Load instructions into table
        List<Map<String, Object>> instructions = (List<Map<String, Object>>) programData.get("instructions");
        if (instructions != null) {
            instructionRows.clear();

            for (Map<String, Object> instData : instructions) {
                int number = ((Number) instData.get("number")).intValue();
                String type = (String) instData.get("type");
                String name = (String) instData.get("name");
                String variable = (String) instData.get("variable");
                String label = (String) instData.get("label");
                int cycles = ((Number) instData.get("cycles")).intValue();
                int degree = ((Number) instData.get("degree")).intValue();

                // Format architecture generation
                String architecture = getArchitectureForInstruction(name);

                // Format instruction display
                String instructionText = formatInstruction(name, variable, instData);

                InstructionRow row = new InstructionRow(
                    number, type, architecture, label != null ? label : "",
                    instructionText, cycles
                );

                instructionRows.add(row);
            }
        }
    }

    private String getArchitectureForInstruction(String instructionName) {
        // Map instruction names to architecture generations
        switch (instructionName) {
            case "NEUTRAL":
            case "INCREASE":
            case "DECREASE":
            case "JUMP_NOT_ZERO":
                return "I";
            case "ZERO_VARIABLE":
            case "CONSTANT_ASSIGNMENT":
            case "GOTO_LABEL":
                return "II";
            case "ASSIGNMENT":
            case "JUMP_ZERO":
            case "JUMP_EQUAL_CONSTANT":
            case "JUMP_EQUAL_VARIABLE":
                return "III";
            case "QUOTE":
            case "JUMP_EQUAL_FUNCTION":
                return "IV";
            default:
                return "?";
        }
    }

    private String formatInstruction(String name, String variable, Map<String, Object> instData) {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (variable != null && !variable.isEmpty()) {
            sb.append(" ").append(variable);
        }

        // Add arguments if present
        Map<String, Object> arguments = (Map<String, Object>) instData.get("arguments");
        if (arguments != null && !arguments.isEmpty()) {
            sb.append(" (");
            boolean first = true;
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            sb.append(")");
        }

        return sb.toString();
    }

    @FXML
    public void initialize() {
        httpClient = SEmulatorClientApp.getHttpClient();
        currentUser = SEmulatorClientApp.getCurrentUser();

        setupControls();
        setupTable();
        disableDebugControls();
        updateCreditsDisplay();
    }

    private void setupControls() {
        // Architecture selector
        architectureSelector.setItems(FXCollections.observableArrayList("I", "II", "III", "IV"));
        architectureSelector.setValue("I");
        architectureSelector.setOnAction(e -> {
            selectedArchitecture = architectureSelector.getValue();
            updateArchitectureSummary();
        });

        // Degree spinner
        degreeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));
        degreeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentDegree = newVal;
            degreeLabel.setText("Degree: " + currentDegree + " / ?");
        });
    }

    private void setupTable() {
        // Breakpoint column with checkboxes
        colBreakpoint.setCellValueFactory(cellData -> cellData.getValue().breakpointProperty());
        colBreakpoint.setCellFactory(col -> new CheckBoxTableCell<>());

        colNumber.setCellValueFactory(cellData -> cellData.getValue().numberProperty().asObject());
        colType.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        colArchitecture.setCellValueFactory(cellData -> cellData.getValue().architectureProperty());
        colLabel.setCellValueFactory(cellData -> cellData.getValue().labelProperty());
        colInstruction.setCellValueFactory(cellData -> cellData.getValue().instructionProperty());
        colCycles.setCellValueFactory(cellData -> cellData.getValue().cyclesProperty().asObject());

        instructionsTable.setItems(instructionRows);

        // Row highlighting
        instructionsTable.setRowFactory(tv -> {
            TableRow<InstructionRow> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null && newItem.isCurrentInstruction()) {
                    row.setStyle("-fx-background-color: #90EE90;");
                } else {
                    row.setStyle("");
                }
            });
            return row;
        });
    }

    @FXML
    private void handleExpand() {
        if (currentDegree < 10) {
            degreeSpinner.getValueFactory().setValue(currentDegree + 1);
        }
    }

    @FXML
    private void handleCollapse() {
        if (currentDegree > 0) {
            degreeSpinner.getValueFactory().setValue(currentDegree - 1);
        }
    }

    @FXML
    private void handleRun() {
        List<Integer> inputs = parseInputs();

        Map<String, Object> request = new HashMap<>();
        request.put("username", currentUser);
        request.put("programName", programName);
        request.put("architecture", selectedArchitecture);
        request.put("degree", currentDegree);
        request.put("inputs", inputs);

        HttpClientService.Response<Map<String, Object>> response = httpClient.execute(request);

        if (response.isSuccess()) {
            Map<String, Object> result = response.getData();
            displayExecutionResult(result);
            updateCreditsDisplay();
            showAlert(Alert.AlertType.INFORMATION, "Execution Complete",
                "Program executed successfully\nOutput: " + result.get("output"));
        } else {
            showAlert(Alert.AlertType.ERROR, "Execution Failed", response.getError());
        }
    }

    @FXML
    private void handleDebug() {
        List<Integer> inputs = parseInputs();
        List<Integer> breakpointsList = new ArrayList<>(breakpoints);

        Map<String, Object> request = new HashMap<>();
        request.put("username", currentUser);
        request.put("programName", programName);
        request.put("architecture", selectedArchitecture);
        request.put("degree", currentDegree);
        request.put("inputs", inputs);
        request.put("breakpoints", breakpointsList);

        HttpClientService.Response<Map<String, Object>> response = httpClient.startDebug(request);

        if (response.isSuccess()) {
            Map<String, Object> result = response.getData();
            debugSessionId = (String) result.get("sessionId");
            isDebugging = true;
            enableDebugControls();
            updateDebugState(result);
            startCreditPolling();
        } else {
            showAlert(Alert.AlertType.ERROR, "Debug Failed", response.getError());
        }
    }

    @FXML
    private void handleStepOver() {
        if (!isDebugging) return;

        HttpClientService.Response<Map<String, Object>> response =
            httpClient.debugStep(debugSessionId, currentUser);

        if (response.isSuccess()) {
            updateDebugState(response.getData());
        } else {
            showAlert(Alert.AlertType.ERROR, "Step Failed", response.getError());
        }
    }

    @FXML
    private void handleStepBack() {
        if (!isDebugging) return;

        HttpClientService.Response<Map<String, Object>> response =
            httpClient.debugStepBack(debugSessionId);

        if (response.isSuccess()) {
            updateDebugState(response.getData());
        } else {
            showAlert(Alert.AlertType.ERROR, "Step Back Failed", response.getError());
        }
    }

    @FXML
    private void handleResume() {
        if (!isDebugging) return;

        HttpClientService.Response<Map<String, Object>> response =
            httpClient.debugResume(debugSessionId, currentUser);

        if (response.isSuccess()) {
            Map<String, Object> result = response.getData();
            updateDebugState(result);

            if ((Boolean) result.get("finished")) {
                stopDebugging();
                showAlert(Alert.AlertType.INFORMATION, "Debug Complete",
                    "Program execution finished");
            } else if ((Boolean) result.get("stoppedAtBreakpoint")) {
                showAlert(Alert.AlertType.INFORMATION, "Breakpoint",
                    "Stopped at breakpoint");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Resume Failed", response.getError());
        }
    }

    @FXML
    private void handleStop() {
        if (!isDebugging) return;

        httpClient.debugStop(debugSessionId);
        stopDebugging();
    }

    @FXML
    private void handleBackToDashboard() {
        if (isDebugging) {
            handleStop();
        }
        if (creditUpdateService != null) {
            creditUpdateService.shutdown();
        }

        Stage stage = (Stage) backToDashboardBtn.getScene().getWindow();
        stage.close();
    }

    private void updateDebugState(Map<String, Object> state) {
        // Update current line highlighting
        int currentLine = ((Number) state.get("currentLine")).intValue();
        for (InstructionRow row : instructionRows) {
            row.setCurrentInstruction(row.getNumber() == currentLine + 1);
        }
        instructionsTable.refresh();

        // Update variables
        Map<String, Object> variables = (Map<String, Object>) state.get("variables");
        displayVariables(variables);

        // Update cycles
        int cycles = ((Number) state.get("cycles")).intValue();
        cyclesLabelBottom.setText("Cycles: " + cycles);

        // Update step back button
        boolean canStepBack = (Boolean) state.getOrDefault("canStepBack", false);
        stepBackBtn.setDisable(!canStepBack);

        // Check if finished
        if ((Boolean) state.get("finished")) {
            stopDebugging();
        }

        // Update credits
        if (state.containsKey("remainingCredits")) {
            creditsLabel.setText("Credits: " + state.get("remainingCredits"));
        }
    }

    private void displayExecutionResult(Map<String, Object> result) {
        Map<String, Object> variables = (Map<String, Object>) result.get("variables");
        displayVariables(variables);

        int cycles = ((Number) result.get("cycles")).intValue();
        cyclesLabelBottom.setText("Cycles: " + cycles);
    }

    private void displayVariables(Map<String, Object> variables) {
        StringBuilder sb = new StringBuilder();
        List<String> keys = new ArrayList<>(variables.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            sb.append(key).append(" = ").append(variables.get(key)).append("\n");
        }

        variablesArea.setText(sb.toString());
    }

    private void startCreditPolling() {
        creditUpdateService = Executors.newSingleThreadScheduledExecutor();
        creditUpdateService.scheduleAtFixedRate(() -> {
            Platform.runLater(this::updateCreditsDisplay);
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void updateCreditsDisplay() {
        HttpClientService.Response<List<Map<String, Object>>> response = httpClient.getUsers();
        if (response.isSuccess()) {
            for (Map<String, Object> user : response.getData()) {
                if (currentUser.equals(user.get("username"))) {
                    creditsLabel.setText("Credits: " + user.get("credits"));
                    break;
                }
            }
        }
    }

    private void updateArchitectureSummary() {
        // This would show instruction counts per architecture
        // For now, just display selected architecture
        architectureSummaryLabel.setText("Selected Architecture: Generation " + selectedArchitecture);
    }

    private void enableDebugControls() {
        runBtn.setDisable(true);
        debugBtn.setDisable(true);
        stepOverBtn.setDisable(false);
        stepBackBtn.setDisable(false);
        resumeBtn.setDisable(false);
        stopBtn.setDisable(false);
    }

    private void disableDebugControls() {
        runBtn.setDisable(false);
        debugBtn.setDisable(false);
        stepOverBtn.setDisable(true);
        stepBackBtn.setDisable(true);
        resumeBtn.setDisable(true);
        stopBtn.setDisable(true);
    }

    private void stopDebugging() {
        isDebugging = false;
        disableDebugControls();
        if (creditUpdateService != null) {
            creditUpdateService.shutdown();
        }

        // Clear highlighting
        for (InstructionRow row : instructionRows) {
            row.setCurrentInstruction(false);
        }
        instructionsTable.refresh();
    }

    private List<Integer> parseInputs() {
        List<Integer> inputs = new ArrayList<>();
        String inputText = inputsArea.getText().trim();
        if (!inputText.isEmpty()) {
            String[] parts = inputText.split("[,\\s]+");
            for (String part : parts) {
                try {
                    inputs.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException e) {
                    // Skip invalid inputs
                }
            }
        }
        return inputs;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Inner class for table rows
    public static class InstructionRow {
        private final BooleanProperty breakpoint = new SimpleBooleanProperty(false);
        private final IntegerProperty number = new SimpleIntegerProperty();
        private final StringProperty type = new SimpleStringProperty();
        private final StringProperty architecture = new SimpleStringProperty();
        private final StringProperty label = new SimpleStringProperty();
        private final StringProperty instruction = new SimpleStringProperty();
        private final IntegerProperty cycles = new SimpleIntegerProperty();
        private boolean currentInstruction = false;

        public InstructionRow(int number, String type, String architecture, String label,
                             String instruction, int cycles) {
            this.number.set(number);
            this.type.set(type);
            this.architecture.set(architecture);
            this.label.set(label);
            this.instruction.set(instruction);
            this.cycles.set(cycles);
        }

        public BooleanProperty breakpointProperty() { return breakpoint; }
        public IntegerProperty numberProperty() { return number; }
        public StringProperty typeProperty() { return type; }
        public StringProperty architectureProperty() { return architecture; }
        public StringProperty labelProperty() { return label; }
        public StringProperty instructionProperty() { return instruction; }
        public IntegerProperty cyclesProperty() { return cycles; }

        public int getNumber() { return number.get(); }
        public boolean isCurrentInstruction() { return currentInstruction; }
        public void setCurrentInstruction(boolean value) { currentInstruction = value; }
    }
}
