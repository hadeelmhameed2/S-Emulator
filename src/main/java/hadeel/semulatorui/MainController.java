package hadeel.semulatorui;

import hadeel.engine.*;
import hadeel.engine.model.*;
import hadeel.engine.parser.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.net.URL;
import java.util.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.TableCell;

public class MainController implements Initializable {

    @FXML private Label filePathLabel;
    @FXML private ComboBox<String> programSelector;
    @FXML private ComboBox<String> themeSelector;
    @FXML private Label currentDegreeLabel;
    @FXML private TextField highlightField;

    @FXML private TableView<InstructionRow> instructionsTable;
    @FXML private TableColumn<InstructionRow, Boolean> colBreakpoint;
    @FXML private TableColumn<InstructionRow, Integer> colNumber;
    @FXML private TableColumn<InstructionRow, String> colType;
    @FXML private TableColumn<InstructionRow, String> colLabel;
    @FXML private TableColumn<InstructionRow, String> colInstruction;
    @FXML private TableColumn<InstructionRow, Integer> colCycles;

    @FXML private Label summaryLabel;
    @FXML private TableView<InstructionRow> historyChainTable;

    @FXML private TextArea variablesArea;
    @FXML private TextArea inputsArea;
    @FXML private Label cyclesLabel;

    @FXML private TableView<ExecutionHistoryRow> historyTable;
    @FXML private TableColumn<ExecutionHistoryRow, Integer> colRunNumber;
    @FXML private TableColumn<ExecutionHistoryRow, Integer> colDegree;
    @FXML private TableColumn<ExecutionHistoryRow, Integer> colOutput;
    @FXML private TableColumn<ExecutionHistoryRow, Integer> colCyclesUsed;

    @FXML private Button loadFileBtn;
    @FXML private Button collapseBtn;
    @FXML private Button expandBtn;
    @FXML private Button runBtn;
    @FXML private Button debugBtn;
    @FXML private Button stepOverBtn;
    @FXML private Button stepBackBtn;
    @FXML private Button stopBtn;
    @FXML private Button resumeBtn;
    @FXML private Button showStatusBtn;
    @FXML private Button rerunBtn;

    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private CheckBox animationsCheckBox;

    private SEmulatorEngine engine;
    private SProgram currentProgram;
    private int currentDegree = 0;
    private boolean isDebugging = false;
    private ExecutionDebugger debugger;
    private ObservableList<InstructionRow> instructionRows;
    private ObservableList<ExecutionHistoryRow> historyRows;
    private Set<Integer> breakpoints;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        engine = new SEmulatorEngine();
        instructionRows = FXCollections.observableArrayList();
        historyRows = FXCollections.observableArrayList();
        breakpoints = new HashSet<>();

        setupTables();
        setupButtons();
        setupThemeSelector();

        progressBar.setVisible(false);
        progressLabel.setVisible(false);

        debugBtn.setDisable(true);
        stepOverBtn.setDisable(true);
        stepBackBtn.setDisable(true);
        stopBtn.setDisable(true);
        resumeBtn.setDisable(true);

        filePathLabel.setText("No file loaded");
        summaryLabel.setText("");
        cyclesLabel.setText("Cycles: 0");

        // Setup animations
        if (animationsCheckBox != null) {
            animationsCheckBox.setSelected(AnimationHelper.isAnimationsEnabled());
            animationsCheckBox.setOnAction(e ->
                AnimationHelper.setAnimationsEnabled(animationsCheckBox.isSelected()));
        }
    }

    private void setupTables() {
        // Setup breakpoint column with checkboxes
        colBreakpoint.setCellValueFactory(new PropertyValueFactory<>("breakpoint"));
        colBreakpoint.setCellFactory(col -> new TableCell<InstructionRow, Boolean>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(e -> {
                    InstructionRow row = getTableRow().getItem();
                    if (row != null) {
                        boolean isSelected = checkBox.isSelected();
                        row.setBreakpoint(isSelected);
                        if (isSelected) {
                            breakpoints.add(row.getNumber() - 1); // Convert to 0-based index
                        } else {
                            breakpoints.remove(row.getNumber() - 1);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item != null && item);
                    setGraphic(checkBox);
                }
            }
        });

        colNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colInstruction.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        colCycles.setCellValueFactory(new PropertyValueFactory<>("cycles"));

        instructionsTable.setItems(instructionRows);
        instructionsTable.setRowFactory(tv -> {
            TableRow<InstructionRow> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    if (newItem.isHighlighted()) {
                        row.setStyle("-fx-background-color: #ffff99;");
                    } else if (newItem.isCurrentInstruction()) {
                        row.setStyle("-fx-background-color: #99ff99;");
                    } else {
                        row.setStyle("");
                    }
                }
            });
            return row;
        });

        historyChainTable.getColumns().addAll(
            createColumn("Number", "number", 80),
            createColumn("Type", "type", 60),
            createColumn("Label", "label", 80),
            createColumn("Instruction", "instruction", 300),
            createColumn("Cycles", "cycles", 80)
        );

        // Add selection listener to instructions table to show history chain
        instructionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            showInstructionHistoryChain(newSelection);
        });

        colRunNumber.setCellValueFactory(new PropertyValueFactory<>("runNumber"));
        colDegree.setCellValueFactory(new PropertyValueFactory<>("degree"));
        colOutput.setCellValueFactory(new PropertyValueFactory<>("output"));
        colCyclesUsed.setCellValueFactory(new PropertyValueFactory<>("cyclesUsed"));

        historyTable.setItems(historyRows);
    }

    private <T> TableColumn<InstructionRow, T> createColumn(String title, String property, double width) {
        TableColumn<InstructionRow, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        return col;
    }

    private void setupButtons() {
        loadFileBtn.setOnAction(e -> loadFile());
        collapseBtn.setOnAction(e -> changeDegree(-1));
        expandBtn.setOnAction(e -> changeDegree(1));
        runBtn.setOnAction(e -> runProgram(false));
        debugBtn.setOnAction(e -> runProgram(true));
        stepOverBtn.setOnAction(e -> {
            if (isDebugging && debugger != null) {
                stepDebugger();
            }
        });
        stepBackBtn.setOnAction(e -> {
            if (isDebugging && debugger != null) {
                stepBackDebugger();
            }
        });
        stopBtn.setOnAction(e -> {
            if (isDebugging) {
                stopDebugger();
            }
        });
        resumeBtn.setOnAction(e -> {
            if (isDebugging && debugger != null) {
                resumeDebugger();
            }
        });

        programSelector.setOnAction(e -> selectProgram());
        highlightField.textProperty().addListener((obs, old, text) -> highlightInstructions(text));

        historyTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showStatusBtn.setDisable(newVal == null);
            rerunBtn.setDisable(newVal == null);
        });

        showStatusBtn.setOnAction(e -> showHistoryStatus());
        rerunBtn.setOnAction(e -> rerunFromHistory());
    }

    private void setupThemeSelector() {
        themeSelector.setItems(FXCollections.observableArrayList(ThemeManager.getAvailableThemes()));
        themeSelector.setValue(ThemeManager.getCurrentTheme());
        themeSelector.setOnAction(e -> {
            String selectedTheme = themeSelector.getValue();
            if (selectedTheme != null) {
                ThemeManager.applyTheme(ThemeManager.getThemeByName(selectedTheme));
            }
        });
    }

    @FXML
    private void loadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open S-Program XML File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );

        File file = fileChooser.showOpenDialog(loadFileBtn.getScene().getWindow());
        if (file != null) {
            loadProgramFile(file);
        }
    }

    private void loadProgramFile(File file) {
        progressBar.setVisible(true);
        progressLabel.setVisible(true);
        progressLabel.setText("Loading file...");

        Task<ParseResult> loadTask = new Task<>() {
            @Override
            protected ParseResult call() throws Exception {
                updateProgress(0, 100);
                Thread.sleep(500);

                updateProgress(30, 100);
                ParseResult result = engine.loadProgram(file.getAbsolutePath());

                updateProgress(70, 100);
                Thread.sleep(500);

                updateProgress(100, 100);
                Thread.sleep(500);

                return result;
            }
        };

        progressBar.progressProperty().bind(loadTask.progressProperty());

        loadTask.setOnSucceeded(e -> {
            ParseResult result = loadTask.getValue();
            if (result.isSuccess()) {
                currentProgram = engine.getCurrentProgram();
                filePathLabel.setText(file.getAbsolutePath());
                AnimationHelper.fadeIn(filePathLabel);
                currentDegree = 0;
                populateProgramSelector();
                displayProgram();
                showSuccessAlert("Program loaded successfully!");
                runBtn.setDisable(false);
                debugBtn.setDisable(false);
            } else {
                showErrorAlert("Failed to load file", result.getErrorMessage());
            }

            progressBar.setVisible(false);
            progressLabel.setVisible(false);
        });

        loadTask.setOnFailed(e -> {
            showErrorAlert("Failed to load file", loadTask.getException().getMessage());
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
        });

        new Thread(loadTask).start();
    }

    private void populateProgramSelector() {
        if (currentProgram == null) return;

        ObservableList<String> programs = FXCollections.observableArrayList();
        programs.add(currentProgram.getName());

        if (currentProgram.getFunctions() != null) {
            for (SFunction func : currentProgram.getFunctions()) {
                programs.add(func.getUserString());
            }
        }

        programSelector.setItems(programs);
        programSelector.getSelectionModel().select(0);
    }

    private void selectProgram() {
        if (currentProgram == null) return;

        int index = programSelector.getSelectionModel().getSelectedIndex();
        if (index < 0) return; // Invalid selection, ignore

        if (index == 0) {
            displayProgram();
        } else {
            // Check if the function index is valid
            if (index - 1 < currentProgram.getFunctions().size()) {
                SFunction func = currentProgram.getFunctions().get(index - 1);
                displayFunction(func);
            }
        }
    }

    private void displayProgram() {
        if (currentProgram == null) return;

        SProgram displayProgram = currentDegree > 0 ?
            engine.expandProgram(currentDegree) : currentProgram;

        instructionRows.clear();
        int num = 1;
        for (SInstruction inst : displayProgram.getInstructions()) {
            instructionRows.add(new InstructionRow(num++, inst));
        }

        updateSummary();
        updateDegreeLabel();
    }

    private void displayFunction(SFunction function) {
        instructionRows.clear();
        int num = 1;
        for (SInstruction inst : function.getInstructions()) {
            instructionRows.add(new InstructionRow(num++, inst));
        }

        summaryLabel.setText("Function: " + function.getName());
    }

    private void changeDegree(int delta) {
        if (currentProgram == null) return;

        int newDegree = currentDegree + delta;
        if (newDegree >= 0 && newDegree <= currentProgram.getMaxDegree()) {
            currentDegree = newDegree;
            displayProgram();
        }
    }

    private void updateDegreeLabel() {
        if (currentProgram != null) {
            currentDegreeLabel.setText(
                String.format("Degree: %d / %d", currentDegree, currentProgram.getMaxDegree())
            );
        }
    }

    private void updateSummary() {
        if (currentProgram == null) return;

        int basic = currentProgram.countBasicInstructions();
        int synthetic = currentProgram.countSyntheticInstructions();
        int total = basic + synthetic;

        summaryLabel.setText(String.format(
            "Total: %d instructions (Basic: %d, Synthetic: %d)",
            total, basic, synthetic
        ));
    }

    private void highlightInstructions(String text) {
        if (text == null || text.trim().isEmpty()) {
            for (InstructionRow row : instructionRows) {
                row.setHighlighted(false);
            }
        } else {
            for (InstructionRow row : instructionRows) {
                SInstruction inst = row.getInstructionObj();
                boolean highlight = false;

                if (inst.getLabel() != null && inst.getLabel().contains(text)) {
                    highlight = true;
                }

                if (inst.getVariable() != null && inst.getVariable().contains(text)) {
                    highlight = true;
                }

                for (String arg : inst.getArguments().values()) {
                    if (arg != null && arg.contains(text)) {
                        highlight = true;
                    }
                }

                row.setHighlighted(highlight);
            }
        }
        instructionsTable.refresh();
    }

    private void runProgram(boolean debug) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Input Values");
        dialog.setHeaderText("Enter input values (comma-separated)");

        Set<String> inputVars = engine.getInputVariables();
        if (!inputVars.isEmpty()) {
            dialog.setContentText("Used variables: " + String.join(", ", inputVars));
        }

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            List<Integer> inputs = parseInputs(result.get());

            if (debug) {
                startDebugMode(inputs);
            } else {
                executeProgram(inputs);
            }
        }
    }

    private List<Integer> parseInputs(String inputStr) {
        List<Integer> inputs = new ArrayList<>();
        if (!inputStr.trim().isEmpty()) {
            try {
                for (String part : inputStr.split(",")) {
                    inputs.add(Integer.parseInt(part.trim()));
                }
            } catch (NumberFormatException e) {
                showErrorAlert("Invalid Input", "Please enter valid numbers separated by commas");
            }
        }
        return inputs;
    }

    private void executeProgram(List<Integer> inputs) {
        ExecutionResult result = engine.executeProgram(inputs, currentDegree);
        if (result != null) {
            displayExecutionResult(result);
            addToHistory(result);
        }
    }

    private void startDebugMode(List<Integer> inputs) {
        isDebugging = true;
        debugger = new ExecutionDebugger(engine, currentProgram, inputs, currentDegree);
        debugger.setBreakpoints(breakpoints);

        stepOverBtn.setDisable(false);
        stepBackBtn.setDisable(false);
        stopBtn.setDisable(false);
        resumeBtn.setDisable(false);
        runBtn.setDisable(true);
        debugBtn.setDisable(true);

        updateDebugDisplay();
    }

    private void stepDebugger() {
        if (debugger != null && !debugger.isFinished()) {
            debugger.step();
            updateDebugDisplay();

            if (debugger.isFinished()) {
                ExecutionResult result = debugger.getResult();
                endDebugMode();
                if (result != null) {
                    displayExecutionResult(result);
                    addToHistory(result);
                }
            }
        }
    }

    private void stepBackDebugger() {
        if (debugger != null && debugger.canStepBack()) {
            debugger.stepBack();
            updateDebugDisplay();
        }
    }

    private void stopDebugger() {
        endDebugMode();
    }

    private void resumeDebugger() {
        if (debugger != null && !debugger.isFinished()) {
            // Execute until breakpoint or finish
            int maxSteps = 10000; // Safety limit to prevent infinite loops
            int steps = 0;

            while (!debugger.isFinished() && steps < maxSteps) {
                debugger.step();
                steps++;

                // Stop at breakpoint
                if (debugger.isAtBreakpoint()) {
                    updateDebugDisplay();
                    return;
                }
            }

            if (steps >= maxSteps) {
                showErrorAlert("Execution Error", "Program exceeded maximum steps limit. Possible infinite loop.");
                endDebugMode();
                return;
            }

            updateDebugDisplay();
            ExecutionResult result = debugger.getResult();
            endDebugMode();
            if (result != null) {
                displayExecutionResult(result);
                addToHistory(result);
            }
        }
    }

    private void endDebugMode() {
        isDebugging = false;
        debugger = null;

        stepOverBtn.setDisable(true);
        stepBackBtn.setDisable(true);
        stopBtn.setDisable(true);
        resumeBtn.setDisable(true);
        runBtn.setDisable(false);
        debugBtn.setDisable(false);

        for (InstructionRow row : instructionRows) {
            row.setCurrentInstruction(false);
        }
        instructionsTable.refresh();
    }

    private void updateDebugDisplay() {
        if (debugger == null) return;

        int currentLine = debugger.getCurrentLine();
        for (int i = 0; i < instructionRows.size(); i++) {
            instructionRows.get(i).setCurrentInstruction(i == currentLine);
        }
        instructionsTable.refresh();

        // Animate the current instruction row
        if (currentLine >= 0 && currentLine < instructionRows.size()) {
            // Scroll to and highlight the current instruction
            instructionsTable.scrollTo(currentLine);
            Platform.runLater(() -> {
                TableRow<?> row = (TableRow<?>) instructionsTable.lookup(".table-row-cell:nth-child(" + (currentLine + 1) + ")");
                if (row != null) {
                    AnimationHelper.highlightDebugLine(row);
                }
            });
        }

        displayVariables(debugger.getCurrentVariables());
        cyclesLabel.setText("Cycles: " + debugger.getCurrentCycles());
        AnimationHelper.pulse(cyclesLabel);

        // Update step back button state
        stepBackBtn.setDisable(!debugger.canStepBack());
    }

    private void displayExecutionResult(ExecutionResult result) {
        displayVariables(result.getFinalVariables());
        cyclesLabel.setText("Cycles: " + result.getCyclesConsumed());
    }

    private void displayVariables(Map<String, Integer> variables) {
        StringBuilder sb = new StringBuilder();

        List<String> sortedVars = new ArrayList<>(variables.keySet());
        sortedVars.sort((a, b) -> {
            if (a.equals("y")) return -1;
            if (b.equals("y")) return 1;
            return a.compareTo(b);
        });

        for (String var : sortedVars) {
            sb.append(var).append(" = ").append(variables.get(var)).append("\n");
        }

        variablesArea.setText(sb.toString());
    }

    private void addToHistory(ExecutionResult result) {
        int runNumber = historyRows.size() + 1;
        historyRows.add(new ExecutionHistoryRow(
            runNumber,
            result.getDegree(),
            result.getOutputValue(),
            result.getCyclesConsumed(),
            result
        ));
    }

    private void showHistoryStatus() {
        ExecutionHistoryRow selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Show variables in a popup dialog instead of overwriting current display
            Map<String, Integer> variables = selected.getResult().getFinalVariables();

            StringBuilder sb = new StringBuilder();
            sb.append("Variables from Run #").append(selected.getRunNumber()).append(":\n\n");

            List<String> sortedVars = new ArrayList<>(variables.keySet());
            sortedVars.sort((a, b) -> {
                if (a.equals("y")) return -1;
                if (b.equals("y")) return 1;
                return a.compareTo(b);
            });

            for (String var : sortedVars) {
                sb.append(var).append(" = ").append(variables.get(var)).append("\n");
            }

            sb.append("\nOutput (y): ").append(selected.getOutput());
            sb.append("\nCycles Used: ").append(selected.getCyclesUsed());
            sb.append("\nDegree: ").append(selected.getDegree());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Execution History - Run #" + selected.getRunNumber());
            alert.setHeaderText("Variable Values");
            alert.setContentText(sb.toString());

            // Make the dialog resizable and bigger
            alert.setResizable(true);
            alert.getDialogPane().setPrefSize(400, 300);

            // Add fade animation
            Platform.runLater(() -> {
                if (alert.getDialogPane() != null) {
                    AnimationHelper.fadeIn(alert.getDialogPane());
                }
            });

            alert.showAndWait();
        }
    }

    private void rerunFromHistory() {
        ExecutionHistoryRow selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            currentDegree = selected.getDegree();
            displayProgram();

            List<Integer> inputs = selected.getResult().getInputs();
            StringBuilder inputStr = new StringBuilder();
            for (int i = 0; i < inputs.size(); i++) {
                if (i > 0) inputStr.append(",");
                inputStr.append(inputs.get(i));
            }
            inputsArea.setText(inputStr.toString());
        }
    }

    private void showInstructionHistoryChain(InstructionRow selectedRow) {
        ObservableList<InstructionRow> chainRows = FXCollections.observableArrayList();

        if (selectedRow != null) {
            SInstruction instruction = selectedRow.getInstructionObj();

            // Build the history chain by following parent links
            List<SInstruction> historyChain = new ArrayList<>();
            SInstruction current = instruction;

            // Add the current instruction first
            historyChain.add(current);

            // Follow the parent chain
            while (current.getParent() != null) {
                current = current.getParent();
                historyChain.add(current);
            }

            // Reverse so oldest (original) is at top, newest at bottom
            Collections.reverse(historyChain);

            // Convert to InstructionRow objects
            for (int i = 0; i < historyChain.size(); i++) {
                SInstruction inst = historyChain.get(i);
                InstructionRow row = new InstructionRow(i + 1, inst);
                chainRows.add(row);
            }
        }

        historyChainTable.setItems(chainRows);

        // If there's a chain, scroll to show the selected instruction (last in chain)
        if (!chainRows.isEmpty()) {
            Platform.runLater(() -> {
                historyChainTable.scrollTo(chainRows.size() - 1);
                historyChainTable.getSelectionModel().select(chainRows.size() - 1);
            });
        }
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Add fade in animation to the alert
        Platform.runLater(() -> {
            if (alert.getDialogPane() != null) {
                AnimationHelper.fadeIn(alert.getDialogPane());
            }
        });

        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class InstructionRow {
        private final SimpleIntegerProperty number;
        private final SimpleStringProperty type;
        private final SimpleStringProperty label;
        private final SimpleStringProperty instruction;
        private final SimpleIntegerProperty cycles;
        private final SInstruction instructionObj;
        private boolean highlighted;
        private boolean currentInstruction;
        private boolean breakpoint;

        public InstructionRow(int number, SInstruction inst) {
            this.number = new SimpleIntegerProperty(number);
            this.type = new SimpleStringProperty(inst.getType().getDisplay());
            this.label = new SimpleStringProperty(inst.getLabel() != null ? inst.getLabel() : "");
            this.instruction = new SimpleStringProperty(inst.getDisplayString());
            this.cycles = new SimpleIntegerProperty(inst.getCycles());
            this.instructionObj = inst;
        }

        public int getNumber() { return number.get(); }
        public String getType() { return type.get(); }
        public String getLabel() { return label.get(); }
        public String getInstruction() { return instruction.get(); }
        public int getCycles() { return cycles.get(); }
        public SInstruction getInstructionObj() { return instructionObj; }

        public boolean isHighlighted() { return highlighted; }
        public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }

        public boolean isCurrentInstruction() { return currentInstruction; }
        public void setCurrentInstruction(boolean current) { this.currentInstruction = current; }

        public boolean getBreakpoint() { return breakpoint; }
        public void setBreakpoint(boolean breakpoint) { this.breakpoint = breakpoint; }
    }

    public static class ExecutionHistoryRow {
        private final SimpleIntegerProperty runNumber;
        private final SimpleIntegerProperty degree;
        private final SimpleIntegerProperty output;
        private final SimpleIntegerProperty cyclesUsed;
        private final ExecutionResult result;

        public ExecutionHistoryRow(int runNumber, int degree, int output, int cycles, ExecutionResult result) {
            this.runNumber = new SimpleIntegerProperty(runNumber);
            this.degree = new SimpleIntegerProperty(degree);
            this.output = new SimpleIntegerProperty(output);
            this.cyclesUsed = new SimpleIntegerProperty(cycles);
            this.result = result;
        }

        public int getRunNumber() { return runNumber.get(); }
        public int getDegree() { return degree.get(); }
        public int getOutput() { return output.get(); }
        public int getCyclesUsed() { return cyclesUsed.get(); }
        public ExecutionResult getResult() { return result; }
    }
}