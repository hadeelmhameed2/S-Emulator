package hadeel;

import hadeel.engine.*;
import hadeel.engine.model.*;
import hadeel.engine.parser.*;
import java.util.*;

public class ConsoleUI {
    private SEmulatorEngine engine;
    private Scanner scanner;
    
    public ConsoleUI() {
        this.engine = new SEmulatorEngine();
        this.scanner = new Scanner(System.in);
    }
    
    public void run() {
        System.out.println("Welcome to S-Emulator V1.0");
        System.out.println("===========================");
        
        boolean running = true;
        while (running) {
            displayMenu();
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    loadXMLFile();
                    break;
                case "2":
                    displayProgram();
                    break;
                case "3":
                    expandProgram();
                    break;
                case "4":
                    runProgram();
                    break;
                case "5":
                    displayHistory();
                    break;
                case "6":
                    saveState();
                    break;
                case "7":
                    loadState();
                    break;
                case "8":
                    running = false;
                    System.out.println("Exiting S-Emulator. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option. Please choose 1-8.");
            }
            
            if (running && !choice.equals("8")) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
        
        scanner.close();
    }
    
private void displayMenu() {
    System.out.println("\n┌───────────────────────────────┐");
    System.out.println("│         S-Emulator Menu       │");
    System.out.println("├───────────────────────────────┤");
    System.out.println("│ 1. Load XML File              │");
    System.out.println("│ 2. Display Program            │");
    System.out.println("│ 3. Expand Program             │");
    System.out.println("│ 4. Run Program                │");
    System.out.println("│ 5. Display History/Statistics │");
    System.out.println("│ 6. Save Emulator State        │");
    System.out.println("│ 7. Load Emulator State        │");
    System.out.println("│ 8. Exit                       │");
    System.out.println("└───────────────────────────────┘");
    System.out.print("Enter your choice: ");
}


    
    private void loadXMLFile() {
        System.out.print("Enter the full path to the XML file: ");
        String filePath = scanner.nextLine().trim().replace("\"", "");
        
        ParseResult result = engine.loadProgram(filePath);
        
        if (result.isSuccess()) {
            System.out.println("Success! Program '" + engine.getProgramName() + 
                             "' loaded successfully.");
        } else {
            System.out.println("Error loading file:");
            System.out.println(result.getErrorMessage());
        }
    }
    
    private void displayProgram() {
        if (!engine.isProgramLoaded()) {
            System.out.println("Error: No program loaded. Please load an XML file first.");
            return;
        }
        
        System.out.println(engine.displayProgram());
    }
    
    private void expandProgram() {
        if (!engine.isProgramLoaded()) {
            System.out.println("Error: No program loaded. Please load an XML file first.");
            return;
        }
        
        int maxDegree = engine.getMaxDegree();
        System.out.println("Maximum degree of the program: " + maxDegree);
        
        int degree = -1;
        while (degree < 0 || degree > maxDegree) {
            System.out.print("Enter expansion degree (0-" + maxDegree + "): ");
            try {
                degree = Integer.parseInt(scanner.nextLine().trim());
                if (degree < 0 || degree > maxDegree) {
                    System.out.println("Error: Degree must be between 0 and " + maxDegree);
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid number");
            }
        }
        
        System.out.println(engine.displayExpandedProgram(degree));
    }
    
    private void runProgram() {
        if (!engine.isProgramLoaded()) {
            System.out.println("Error: No program loaded. Please load an XML file first.");
            return;
        }
        
        int maxDegree = engine.getMaxDegree();
        System.out.println("Maximum degree of the program: " + maxDegree);
        
        int degree = -1;
        while (degree < 0 || degree > maxDegree) {
            System.out.print("Enter execution degree (0-" + maxDegree + "): ");
            try {
                degree = Integer.parseInt(scanner.nextLine().trim());
                if (degree < 0 || degree > maxDegree) {
                    System.out.println("Error: Degree must be between 0 and " + maxDegree);
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid number");
            }
        }
        
        Set<String> inputVars = engine.getInputVariables();
        if (!inputVars.isEmpty()) {
            System.out.println("Program uses input variables: " + String.join(", ", inputVars));
        }
        
        System.out.print("Enter input values (comma-separated, e.g., 5,3,7): ");
        String inputStr = scanner.nextLine().trim();
        
        List<Integer> inputs = new ArrayList<>();
        if (!inputStr.isEmpty()) {
            try {
                String[] parts = inputStr.split(",");
                for (String part : parts) {
                    inputs.add(Integer.parseInt(part.trim()));
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid input format. Using empty input.");
                inputs.clear();
            }
        }
        
        System.out.println("\nExecuting program...");
        ExecutionResult result = engine.executeProgram(inputs, degree);
        
        if (result != null) {
            if (degree > 0) {
                System.out.println("\nExpanded Program (Degree " + degree + "):");
                SProgram expanded = result.getExpandedProgram();
                if (expanded != null) {
                    List<SInstruction> instructions = expanded.getInstructions();
                    for (int i = 0; i < instructions.size(); i++) {
                        System.out.println(instructions.get(i).getFormattedDisplay(i + 1));
                    }
                }
            }
            
            System.out.println("\n=== Execution Results ===");
            System.out.println("Output (y): " + result.getOutputValue());
            
            System.out.println("\nFinal Variable Values:");
            Map<String, Integer> vars = result.getFinalVariables();
            for (Map.Entry<String, Integer> entry : vars.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }
            
            System.out.println("\nTotal Cycles Consumed: " + result.getCyclesConsumed());
        }
    }
    
    private void displayHistory() {
        if (!engine.isProgramLoaded()) {
            System.out.println("Error: No program loaded. Please load an XML file first.");
            return;
        }
        
        List<ExecutionResult> history = engine.getExecutionHistory();
        if (history.isEmpty()) {
            System.out.println("No execution history available.");
            return;
        }
        
        System.out.println(engine.displayExecutionHistory());
    }
    
    private void saveState() {
        if (!engine.isProgramLoaded()) {
            System.out.println("Error: No program loaded. Cannot save empty state.");
            return;
        }
        
        System.out.print("Enter file path (without extension) to save state: ");
        String filePath = scanner.nextLine().trim();
        
        if (filePath.isEmpty()) {
            System.out.println("Error: File path cannot be empty.");
            return;
        }
        
        if (engine.saveState(filePath)) {
            System.out.println("Success! Emulator state saved to: " + filePath + ".semulator");
        } else {
            System.out.println("Error: Failed to save emulator state.");
        }
    }
    
    private void loadState() {
        System.out.print("Enter file path (without extension) to load state from: ");
        String filePath = scanner.nextLine().trim();
        
        if (filePath.isEmpty()) {
            System.out.println("Error: File path cannot be empty.");
            return;
        }
        
        if (engine.loadState(filePath)) {
            System.out.println("Success! Emulator state loaded from: " + filePath + ".semulator");
            if (engine.isProgramLoaded()) {
                System.out.println("Loaded program: " + engine.getProgramName());
                System.out.println("Execution history entries: " + engine.getExecutionHistory().size());
            }
        } else {
            System.out.println("Error: Failed to load emulator state. File may not exist or be corrupted.");
        }
    }
    
    public static void main(String[] args) {
        ConsoleUI ui = new ConsoleUI();
        ui.run();
    }
}