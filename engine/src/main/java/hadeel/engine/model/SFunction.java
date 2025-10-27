package hadeel.engine.model;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class SFunction implements Serializable {
    private String name;
    private String userString;
    private List<SInstruction> instructions;
    
    public SFunction() {
        this.instructions = new ArrayList<>();
    }
    
    public SFunction(String name, String userString) {
        this();
        this.name = name;
        this.userString = userString;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUserString() {
        return userString;
    }
    
    public void setUserString(String userString) {
        this.userString = userString;
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

    public int getMaxDegree() {
        int maxDegree = 0;
        for (SInstruction instruction : instructions) {
            maxDegree = Math.max(maxDegree, instruction.getDegree());
        }
        return maxDegree;
    }
}