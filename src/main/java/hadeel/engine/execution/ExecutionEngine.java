package hadeel.engine.execution;

import hadeel.engine.model.*;
import java.util.*;

public class ExecutionEngine {
    
    public ExecutionResult execute(SProgram program, List<Integer> inputs, int expansionDegree) {
        SProgram expandedProgram = ExpansionEngine.expand(program, expansionDegree);
        
        ExecutionContext context = new ExecutionContext();
        
        Map<String, Integer> inputVariables = new HashMap<>();
        for (int i = 0; i < inputs.size(); i++) {
            inputVariables.put("x" + (i + 1), inputs.get(i));
        }
        context.initializeInputVariables(inputVariables);
        
        List<SInstruction> instructions = expandedProgram.getInstructions();
        
        while (!context.isTerminated() && context.getCurrentInstructionIndex() < instructions.size()) {
            SInstruction currentInstruction = instructions.get(context.getCurrentInstructionIndex());
            executeInstruction(currentInstruction, context, instructions);
        }
        
        ExecutionResult result = new ExecutionResult(
            context.getOutputValue(),
            context.getVariablesSorted(),
            context.getCyclesConsumed(),
            expansionDegree,
            inputs
        );
        result.setExpandedProgram(expandedProgram);
        
        return result;
    }
    
    private void executeInstruction(SInstruction instruction, ExecutionContext context, 
                                   List<SInstruction> instructions) {
        context.addCycles(instruction.getCycles());
        
        switch (instruction.getName()) {
            case INCREASE:
                executeIncrease(instruction, context);
                break;
            case DECREASE:
                executeDecrease(instruction, context);
                break;
            case JUMP_NOT_ZERO:
                executeJumpNotZero(instruction, context, instructions);
                break;
            case NEUTRAL:
                executeNeutral(instruction, context);
                break;
            case ZERO_VARIABLE:
                executeZeroVariable(instruction, context);
                break;
            case GOTO_LABEL:
                executeGotoLabel(instruction, context, instructions);
                break;
            case ASSIGNMENT:
                executeAssignment(instruction, context);
                break;
            case CONSTANT_ASSIGNMENT:
                executeConstantAssignment(instruction, context);
                break;
            case JUMP_ZERO:
                executeJumpZero(instruction, context, instructions);
                break;
            case JUMP_EQUAL_CONSTANT:
                executeJumpEqualConstant(instruction, context, instructions);
                break;
            case JUMP_EQUAL_VARIABLE:
                executeJumpEqualVariable(instruction, context, instructions);
                break;
            default:
                context.nextInstruction();
        }
    }
    
    private void executeIncrease(SInstruction instruction, ExecutionContext context) {
        context.incrementVariable(instruction.getVariable());
        context.nextInstruction();
    }
    
    private void executeDecrease(SInstruction instruction, ExecutionContext context) {
        context.decrementVariable(instruction.getVariable());
        context.nextInstruction();
    }
    
    private void executeJumpNotZero(SInstruction instruction, ExecutionContext context, 
                                   List<SInstruction> instructions) {
        int value = context.getVariable(instruction.getVariable());
        if (value != 0) {
            String targetLabel = instruction.getArgument("JNZLabel");
            jumpToLabel(targetLabel, context, instructions);
        } else {
            context.nextInstruction();
        }
    }
    
    private void executeNeutral(SInstruction instruction, ExecutionContext context) {
        context.nextInstruction();
    }
    
    private void executeZeroVariable(SInstruction instruction, ExecutionContext context) {
        context.setVariable(instruction.getVariable(), 0);
        context.nextInstruction();
    }
    
    private void executeGotoLabel(SInstruction instruction, ExecutionContext context, 
                                 List<SInstruction> instructions) {
        String targetLabel = instruction.getArgument("gotoLabel");
        jumpToLabel(targetLabel, context, instructions);
    }
    
    private void executeAssignment(SInstruction instruction, ExecutionContext context) {
        String sourceVar = instruction.getArgument("assignedVariable");
        int value = context.getVariable(sourceVar);
        context.setVariable(instruction.getVariable(), value);
        context.nextInstruction();
    }
    
    private void executeConstantAssignment(SInstruction instruction, ExecutionContext context) {
        String constantStr = instruction.getArgument("constantValue");
        int value = Integer.parseInt(constantStr);
        context.setVariable(instruction.getVariable(), value);
        context.nextInstruction();
    }
    
    private void executeJumpZero(SInstruction instruction, ExecutionContext context, 
                                List<SInstruction> instructions) {
        int value = context.getVariable(instruction.getVariable());
        if (value == 0) {
            String targetLabel = instruction.getArgument("JZLabel");
            jumpToLabel(targetLabel, context, instructions);
        } else {
            context.nextInstruction();
        }
    }
    
    private void executeJumpEqualConstant(SInstruction instruction, ExecutionContext context, 
                                         List<SInstruction> instructions) {
        int varValue = context.getVariable(instruction.getVariable());
        int constantValue = Integer.parseInt(instruction.getArgument("constantValue"));
        
        if (varValue == constantValue) {
            String targetLabel = instruction.getArgument("JEConstantLabel");
            jumpToLabel(targetLabel, context, instructions);
        } else {
            context.nextInstruction();
        }
    }
    
    private void executeJumpEqualVariable(SInstruction instruction, ExecutionContext context, 
                                         List<SInstruction> instructions) {
        int value1 = context.getVariable(instruction.getVariable());
        int value2 = context.getVariable(instruction.getArgument("variableName"));
        
        if (value1 == value2) {
            String targetLabel = instruction.getArgument("JEVariableLabel");
            jumpToLabel(targetLabel, context, instructions);
        } else {
            context.nextInstruction();
        }
    }
    
    private void jumpToLabel(String label, ExecutionContext context, List<SInstruction> instructions) {
        if ("EXIT".equals(label)) {
            context.terminate();
            return;
        }
        
        for (int i = 0; i < instructions.size(); i++) {
            if (label.equals(instructions.get(i).getLabel())) {
                context.setCurrentInstructionIndex(i);
                return;
            }
        }
        
        context.nextInstruction();
    }
}