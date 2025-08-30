package hadeel.engine;

import hadeel.engine.model.*;
import hadeel.engine.parser.*;
import hadeel.engine.execution.*;
import java.util.*;
import java.io.*;

public class SEmulatorEngine {
    private SProgram currentProgram;
    private List<ExecutionResult> executionHistory;
    
    public SEmulatorEngine() {
        this.executionHistory = new ArrayList<>();
    }
    
    public ParseResult loadProgram(String filePath) {
        ParseResult result = XMLParser.parseFile(filePath);
        if (result.isSuccess()) {
            currentProgram = result.getProgram();
            executionHistory.clear();
        }
        return result;
    }
    
    public boolean isProgramLoaded() {
        return currentProgram != null;
    }
    
    public SProgram getCurrentProgram() {
        return currentProgram;
    }
    
    public String getProgramName() {
        if (currentProgram == null) {
            return null;
        }
        return currentProgram.getName();
    }
    
    public int getMaxDegree() {
        if (currentProgram == null) {
            return 0;
        }
        return currentProgram.getMaxDegree();
    }
    
    public Set<String> getInputVariables() {
        if (currentProgram == null) {
            return new HashSet<>();
        }
        return currentProgram.getUsedInputVariables();
    }
    
    public Set<String> getLabels() {
        if (currentProgram == null) {
            return new HashSet<>();
        }
        return currentProgram.getUsedLabels();
    }
    
    public String displayProgram() {
        if (currentProgram == null) {
            return "No program loaded";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Program Name: ").append(currentProgram.getName()).append("\n");
        
        Set<String> inputs = getInputVariables();
        if (!inputs.isEmpty()) {
            sb.append("Input Variables: ").append(String.join(", ", inputs)).append("\n");
        } else {
            sb.append("Input Variables: None\n");
        }
        
        Set<String> labels = getLabels();
        if (!labels.isEmpty()) {
            List<String> labelList = new ArrayList<>(labels);
            labelList.remove("EXIT");
            Collections.sort(labelList);
            if (labels.contains("EXIT")) {
                labelList.add("EXIT");
            }
            sb.append("Labels: ").append(String.join(", ", labelList)).append("\n");
        } else {
            sb.append("Labels: None\n");
        }
        
        sb.append("Instructions:\n");
        List<SInstruction> instructions = currentProgram.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            sb.append(instructions.get(i).getFormattedDisplay(i + 1)).append("\n");
        }
        
        return sb.toString();
    }
    
    public SProgram expandProgram(int degree) {
        if (currentProgram == null) {
            return null;
        }
        return ExpansionEngine.expand(currentProgram, degree);
    }
    
    public String displayExpandedProgram(int degree) {
        SProgram expanded = expandProgram(degree);
        if (expanded == null) {
            return "No program loaded";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Expanded to degree ").append(degree).append(":\n");
        
        List<SInstruction> instructions = expanded.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            SInstruction inst = instructions.get(i);
            sb.append(inst.getFormattedDisplay(i + 1));
            
            List<SInstruction> history = new ArrayList<>();
            SInstruction parent = inst.getParent();
            while (parent != null) {
                history.add(parent);
                parent = parent.getParent();
            }
            
            if (!history.isEmpty()) {
                Collections.reverse(history);
                for (SInstruction hist : history) {
                    sb.append(" <<< ").append(hist.getFormattedDisplay(hist.getLineNumber()));
                }
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    public ExecutionResult executeProgram(List<Integer> inputs, int degree) {
        if (currentProgram == null) {
            return null;
        }
        
        ExecutionEngine engine = new ExecutionEngine();
        ExecutionResult result = engine.execute(currentProgram, inputs, degree);
        executionHistory.add(result);
        
        return result;
    }
    
    public List<ExecutionResult> getExecutionHistory() {
        return executionHistory;
    }
    
    public String displayExecutionHistory() {
        if (executionHistory.isEmpty()) {
            return "No execution history";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Execution History:\n");
        
        for (int i = 0; i < executionHistory.size(); i++) {
            ExecutionResult result = executionHistory.get(i);
            sb.append(String.format("Run #%d: Degree=%d, Inputs=%s, y=%d, Cycles=%d\n",
                i + 1,
                result.getDegree(),
                formatInputs(result.getInputs()),
                result.getOutputValue(),
                result.getCyclesConsumed()
            ));
        }
        
        return sb.toString();
    }
    
    private String formatInputs(List<Integer> inputs) {
        if (inputs.isEmpty()) {
            return "None";
        }
        List<String> formatted = new ArrayList<>();
        for (Integer input : inputs) {
            formatted.add(input.toString());
        }
        return String.join(",", formatted);
    }
    
    public void reset() {
        currentProgram = null;
        executionHistory.clear();
    }
    
    public boolean saveState(String filePath) {
        try {
            FileOutputStream fileOut = new FileOutputStream(filePath + ".semulator");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            
            out.writeObject(currentProgram);
            out.writeObject(executionHistory);
            
            out.close();
            fileOut.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    public boolean loadState(String filePath) {
        try {
            FileInputStream fileIn = new FileInputStream(filePath + ".semulator");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            
            currentProgram = (SProgram) in.readObject();
            executionHistory = (List<ExecutionResult>) in.readObject();
            
            in.close();
            fileIn.close();
            return true;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }
}