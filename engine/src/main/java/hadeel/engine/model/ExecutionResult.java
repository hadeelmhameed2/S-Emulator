package hadeel.engine.model;

import java.util.Map;
import java.util.List;
import java.io.Serializable;

public class ExecutionResult implements Serializable {
    private int outputValue;
    private Map<String, Integer> finalVariables;
    private int cyclesConsumed;
    private int degree;
    private List<Integer> inputs;
    private SProgram expandedProgram;
    
    public ExecutionResult(int outputValue, Map<String, Integer> finalVariables, 
                          int cyclesConsumed, int degree, List<Integer> inputs) {
        this.outputValue = outputValue;
        this.finalVariables = finalVariables;
        this.cyclesConsumed = cyclesConsumed;
        this.degree = degree;
        this.inputs = inputs;
    }
    
    public int getOutputValue() {
        return outputValue;
    }
    
    public void setOutputValue(int outputValue) {
        this.outputValue = outputValue;
    }
    
    public Map<String, Integer> getFinalVariables() {
        return finalVariables;
    }
    
    public void setFinalVariables(Map<String, Integer> finalVariables) {
        this.finalVariables = finalVariables;
    }
    
    public int getCyclesConsumed() {
        return cyclesConsumed;
    }
    
    public void setCyclesConsumed(int cyclesConsumed) {
        this.cyclesConsumed = cyclesConsumed;
    }
    
    public int getDegree() {
        return degree;
    }
    
    public void setDegree(int degree) {
        this.degree = degree;
    }
    
    public List<Integer> getInputs() {
        return inputs;
    }
    
    public void setInputs(List<Integer> inputs) {
        this.inputs = inputs;
    }
    
    public SProgram getExpandedProgram() {
        return expandedProgram;
    }
    
    public void setExpandedProgram(SProgram expandedProgram) {
        this.expandedProgram = expandedProgram;
    }
}