package hadeel.engine.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.io.Serializable;

public class SProgram implements Serializable {
    private String name;
    private List<SInstruction> instructions;
    private List<SFunction> functions;
    
    public SProgram() {
        this.instructions = new ArrayList<>();
        this.functions = new ArrayList<>();
    }
    
    public SProgram(String name) {
        this();
        this.name = name;
    }
    
    public int getMaxDegree() {
        int maxDegree = 0;
        for (SInstruction instruction : instructions) {
            maxDegree = Math.max(maxDegree, instruction.getDegree());
        }
        return maxDegree;
    }
    
    public Set<String> getUsedInputVariables() {
        Set<String> variables = new TreeSet<>();
        for (SInstruction instruction : instructions) {
            collectVariables(instruction, variables);
        }
        return variables;
    }
    
    private void collectVariables(SInstruction instruction, Set<String> variables) {
        if (instruction.getVariable() != null && instruction.getVariable().startsWith("x")) {
            variables.add(instruction.getVariable());
        }
        
        for (String value : instruction.getArguments().values()) {
            if (value != null && value.startsWith("x")) {
                variables.add(value);
            }
        }
    }
    
    public Set<String> getUsedLabels() {
        Set<String> labels = new HashSet<>();
        for (SInstruction instruction : instructions) {
            if (instruction.getLabel() != null) {
                labels.add(instruction.getLabel());
            }
            
            String jnzLabel = instruction.getArgument("JNZLabel");
            if (jnzLabel != null) {
                labels.add(jnzLabel);
            }
            
            String gotoLabel = instruction.getArgument("gotoLabel");
            if (gotoLabel != null) {
                labels.add(gotoLabel);
            }
            
            String jzLabel = instruction.getArgument("JZLabel");
            if (jzLabel != null) {
                labels.add(jzLabel);
            }
            
            String jecLabel = instruction.getArgument("JEConstantLabel");
            if (jecLabel != null) {
                labels.add(jecLabel);
            }
            
            String jevLabel = instruction.getArgument("JEVariableLabel");
            if (jevLabel != null) {
                labels.add(jevLabel);
            }
        }
        
        if (hasExitReference()) {
            labels.add("EXIT");
        }
        
        return labels;
    }
    
    private boolean hasExitReference() {
        for (SInstruction instruction : instructions) {
            for (String value : instruction.getArguments().values()) {
                if ("EXIT".equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public int countBasicInstructions() {
        int count = 0;
        for (SInstruction instruction : instructions) {
            if (instruction.getType() == InstructionType.BASIC) {
                count++;
            }
        }
        return count;
    }
    
    public int countSyntheticInstructions() {
        int count = 0;
        for (SInstruction instruction : instructions) {
            if (instruction.getType() == InstructionType.SYNTHETIC) {
                count++;
            }
        }
        return count;
    }
    
    public int getTotalCycles() {
        int total = 0;
        for (SInstruction instruction : instructions) {
            total += instruction.getCycles();
        }
        return total;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<SInstruction> getInstructions() {
        return instructions;
    }
    
    public void setInstructions(List<SInstruction> instructions) {
        this.instructions = instructions;
    }
    
    public void addInstruction(SInstruction instruction) {
        this.instructions.add(instruction);
    }
    
    public List<SFunction> getFunctions() {
        return functions;
    }
    
    public void setFunctions(List<SFunction> functions) {
        this.functions = functions;
    }
    
    public void addFunction(SFunction function) {
        this.functions.add(function);
    }
    
    public SFunction getFunction(String name) {
        for (SFunction function : functions) {
            if (function.getName().equals(name)) {
                return function;
            }
        }
        return null;
    }
}