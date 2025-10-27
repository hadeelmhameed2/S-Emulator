package hadeel.engine.execution;

import hadeel.engine.SEmulatorEngine;
import hadeel.engine.model.*;

import java.util.*;
import java.util.Set;
import java.util.HashSet;

public class ExecutionDebugger {

    // Inner class for storing debug states
    private static class DebugState {
        final int line;
        final Map<String, Integer> variables;
        final int cycles;

        DebugState(int line, Map<String, Integer> variables, int cycles) {
            this.line = line;
            this.variables = new HashMap<>(variables);
            this.cycles = cycles;
        }
    }
    private SEmulatorEngine engine;
    private SProgram program;
    private List<Integer> inputs;
    private int degree;
    private int currentLine;
    private Map<String, Integer> variables;
    private int cyclesConsumed;
    private boolean finished;
    private ExecutionResult result;
    private List<SInstruction> instructions;
    private Set<Integer> breakpoints;
    private List<DebugState> executionHistory;
    private int historyIndex;

    public ExecutionDebugger(SEmulatorEngine engine, SProgram program, List<Integer> inputs, int degree) {
        this.engine = engine;
        this.program = program;
        this.inputs = inputs;
        this.degree = degree;
        this.currentLine = 0;
        this.variables = new HashMap<>();
        this.cyclesConsumed = 0;
        this.finished = false;
        this.breakpoints = new HashSet<>();
        this.executionHistory = new ArrayList<>();
        this.historyIndex = -1;

        initializeVariables();

        // Save initial state
        saveCurrentState();

        if (degree > 0) {
            // Use the program parameter directly, not engine.expandProgram() which depends on engine's currentProgram
            SProgram expanded = ExpansionEngine.expand(program, degree);
            if (expanded != null) {
                this.instructions = expanded.getInstructions();
            } else {
                System.out.println("[ExecutionDebugger] ERROR: Failed to expand program to degree " + degree);
                this.instructions = program.getInstructions();
            }
        } else {
            this.instructions = program.getInstructions();
        }
    }

    public void setBreakpoints(Set<Integer> breakpoints) {
        this.breakpoints = new HashSet<>(breakpoints);
    }

    public boolean isAtBreakpoint() {
        return breakpoints.contains(currentLine);
    }

    private void initializeVariables() {
        variables.put("y", 0);
        variables.put("z", 0);

        for (int i = 1; i <= 10; i++) {
            variables.put("x" + i, i <= inputs.size() ? inputs.get(i - 1) : 0);
        }

        for (int i = 1; i <= 10; i++) {
            variables.put("r" + i, 0);
        }
    }

    public void step() {
        if (finished || currentLine >= instructions.size()) {
            finish();
            return;
        }

        SInstruction inst = instructions.get(currentLine);
        executeInstruction(inst);
        cyclesConsumed += inst.getCycles();
        currentLine++;

        // Save state after step
        saveCurrentState();

        if (currentLine >= instructions.size()) {
            finish();
        }
    }

    public boolean canStepBack() {
        return historyIndex > 0;
    }

    public void stepBack() {
        if (canStepBack()) {
            historyIndex--;
            restoreState(executionHistory.get(historyIndex));
        }
    }

    private void saveCurrentState() {
        DebugState state = new DebugState(currentLine, variables, cyclesConsumed);

        // If we're stepping back and then forward, remove future history
        if (historyIndex < executionHistory.size() - 1) {
            executionHistory = executionHistory.subList(0, historyIndex + 1);
        }

        executionHistory.add(state);
        historyIndex = executionHistory.size() - 1;
    }

    private void restoreState(DebugState state) {
        this.currentLine = state.line;
        this.variables = new HashMap<>(state.variables);
        this.cyclesConsumed = state.cycles;
        this.finished = false;
    }

    private void executeInstruction(SInstruction inst) {
        String var = inst.getVariable();

        switch (inst.getName()) {
            case INCREASE:
                variables.put(var, variables.getOrDefault(var, 0) + 1);
                break;
            case DECREASE:
                variables.put(var, variables.getOrDefault(var, 0) - 1);
                break;
            case NEUTRAL:
                break;
            case JUMP_NOT_ZERO:
                if (variables.getOrDefault(var, 0) != 0) {
                    String label = inst.getArgument("JNZLabel");
                    currentLine = findLabelLine(label) - 1;
                }
                break;
            case ZERO_VARIABLE:
                variables.put(var, 0);
                break;
            case GOTO_LABEL:
                String label = inst.getArgument("gotoLabel");
                if ("EXIT".equals(label)) {
                    currentLine = instructions.size() - 1;
                } else {
                    currentLine = findLabelLine(label) - 1;
                }
                break;
            case ASSIGNMENT:
                String sourceVar = inst.getArgument("assignedVariable");
                variables.put(var, variables.getOrDefault(sourceVar, 0));
                break;
            case CONSTANT_ASSIGNMENT:
                int value = Integer.parseInt(inst.getArgument("constantValue"));
                variables.put(var, value);
                break;
            case JUMP_ZERO:
                if (variables.getOrDefault(var, 0) == 0) {
                    String jzLabel = inst.getArgument("JZLabel");
                    currentLine = findLabelLine(jzLabel) - 1;
                }
                break;
            case JUMP_EQUAL_CONSTANT:
                int constant = Integer.parseInt(inst.getArgument("constantValue"));
                if (variables.getOrDefault(var, 0) == constant) {
                    String jecLabel = inst.getArgument("JEConstantLabel");
                    currentLine = findLabelLine(jecLabel) - 1;
                }
                break;
            case JUMP_EQUAL_VARIABLE:
                String compareVar = inst.getArgument("variableName");
                if (variables.getOrDefault(var, 0) == variables.getOrDefault(compareVar, 0)) {
                    String jevLabel = inst.getArgument("JEVariableLabel");
                    currentLine = findLabelLine(jevLabel) - 1;
                }
                break;
        }
    }

    private int findLabelLine(String label) {
        if ("EXIT".equals(label)) {
            return instructions.size();
        }

        for (int i = 0; i < instructions.size(); i++) {
            if (label != null && label.equals(instructions.get(i).getLabel())) {
                return i;
            }
        }
        // If label not found, continue to next instruction
        return currentLine + 1;
    }

    private void finish() {
        finished = true;
        result = new ExecutionResult(
            variables.get("y"),           // outputValue
            new HashMap<>(variables),      // finalVariables
            cyclesConsumed,                // cyclesConsumed
            degree,                        // degree
            inputs                         // inputs
        );

        if (degree > 0) {
            // Use the program parameter directly, not engine.expandProgram()
            SProgram expandedProgram = ExpansionEngine.expand(program, degree);
            if (expandedProgram != null) {
                result.setExpandedProgram(expandedProgram);
            }
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public Map<String, Integer> getCurrentVariables() {
        return new HashMap<>(variables);
    }

    public int getCurrentCycles() {
        return cyclesConsumed;
    }

    public ExecutionResult getResult() {
        return result;
    }
}