package hadeel.engine.model;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;
import java.io.Serializable;

public class ExecutionContext {
    
    private static class VariableComparator implements Comparator<String>, Serializable {
        @Override
        public int compare(String a, String b) {
            if (a.equals("y")) return -1;
            if (b.equals("y")) return 1;
            
            char typeA = a.charAt(0);
            char typeB = b.charAt(0);
            
            if (typeA != typeB) {
                if (typeA == 'x') return -1;
                if (typeB == 'x') return 1;
            }
            
            try {
                int numA = Integer.parseInt(a.substring(1));
                int numB = Integer.parseInt(b.substring(1));
                return Integer.compare(numA, numB);
            } catch (Exception e) {
                return a.compareTo(b);
            }
        }
    }
    private Map<String, Integer> variables;
    private int currentInstructionIndex;
    private int cyclesConsumed;
    private boolean terminated;
    
    public ExecutionContext() {
        this.variables = new HashMap<>();
        this.currentInstructionIndex = 0;
        this.cyclesConsumed = 0;
        this.terminated = false;
    }
    
    public void initializeInputVariables(Map<String, Integer> inputs) {
        variables.clear();
        variables.putAll(inputs);
        variables.put("y", 0);
    }
    
    public int getVariable(String name) {
        return variables.getOrDefault(name, 0);
    }
    
    public void setVariable(String name, int value) {
        if (value < 0) {
            value = 0;
        }
        variables.put(name, value);
    }
    
    public void incrementVariable(String name) {
        setVariable(name, getVariable(name) + 1);
    }
    
    public void decrementVariable(String name) {
        int value = getVariable(name);
        if (value > 0) {
            setVariable(name, value - 1);
        }
    }
    
    public Map<String, Integer> getVariablesSorted() {
        Map<String, Integer> sorted = new TreeMap<>(new VariableComparator());
        sorted.putAll(variables);
        return sorted;
    }
    
    public Map<String, Integer> getVariables() {
        return variables;
    }
    
    public int getCurrentInstructionIndex() {
        return currentInstructionIndex;
    }
    
    public void setCurrentInstructionIndex(int index) {
        this.currentInstructionIndex = index;
    }
    
    public void nextInstruction() {
        currentInstructionIndex++;
    }
    
    public int getCyclesConsumed() {
        return cyclesConsumed;
    }
    
    public void addCycles(int cycles) {
        this.cyclesConsumed += cycles;
    }
    
    public boolean isTerminated() {
        return terminated;
    }
    
    public void terminate() {
        this.terminated = true;
    }
    
    public int getOutputValue() {
        return getVariable("y");
    }
}