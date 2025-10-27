package hadeel.engine.execution;

import hadeel.engine.model.*;
import java.util.*;

public class ExecutionEngine {
    
    private SProgram currentProgram; // Store program for QUOTE execution

    public ExecutionResult execute(SProgram program, List<Integer> inputs, int expansionDegree) {
        SProgram expandedProgram = ExpansionEngine.expand(program, expansionDegree);
        this.currentProgram = expandedProgram; // Store for QUOTE execution

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
        // Debug logging
        System.out.println("\n[EXEC] Instruction #" + context.getCurrentInstructionIndex() +
                         ": " + instruction.getName() +
                         " var=" + instruction.getVariable() +
                         " label=" + instruction.getLabel() +
                         " args=" + instruction.getArguments());
        System.out.println("[EXEC] Variables BEFORE: " + context.getVariablesSorted());

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
            case QUOTE:
                executeQuote(instruction, context);
                break;
            default:
                System.out.println("[EXEC] WARNING: Unknown instruction type: " + instruction.getName());
                context.nextInstruction();
        }

        System.out.println("[EXEC] Variables AFTER: " + context.getVariablesSorted());
        System.out.println("[EXEC] Next instruction index: " + context.getCurrentInstructionIndex());
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
        String targetLabel = instruction.getArgument("JNZLabel");
        System.out.println("[EXEC] JNZ: value=" + value + ", target=" + targetLabel + ", jumping=" + (value != 0));
        if (value != 0) {
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
        System.out.println("[EXEC] JUMP to label: " + label + " (isEXIT=" + "EXIT".equals(label) + ")");
        if ("EXIT".equals(label)) {
            System.out.println("[EXEC] ***** Terminating ENTIRE PROGRAM *****");
            System.out.println("[EXEC] Final variables: " + context.getVariablesSorted());
            context.terminate();
            return;
        }

        for (int i = 0; i < instructions.size(); i++) {
            if (label.equals(instructions.get(i).getLabel())) {
                System.out.println("[EXEC] Found label '" + label + "' at instruction #" + i);
                context.setCurrentInstructionIndex(i);
                return;
            }
        }

        System.out.println("[EXEC] WARNING: Label '" + label + "' not found, moving to next instruction");
        context.nextInstruction();
    }

    private void executeQuote(SInstruction instruction, ExecutionContext context) {
        executeQuote(instruction, context, true);
    }

    private void executeQuote(SInstruction instruction, ExecutionContext context, boolean advanceInstructionPointer) {
        String functionName = instruction.getArgument("functionName");
        String functionArguments = instruction.getArgument("functionArguments");
        String outputVar = instruction.getVariable();

        System.out.println("[EXEC] QUOTE: calling function " + functionName + " with args '" + functionArguments + "' -> " + outputVar);

        // Find the function
        SFunction function = currentProgram.getFunction(functionName);
        if (function == null) {
            System.out.println("[EXEC] ERROR: Function not found: " + functionName);
            if (advanceInstructionPointer) {
                context.nextInstruction();
            }
            return;
        }

        // Parse arguments
        List<String> argNames = parseQuoteArguments(functionArguments);
        System.out.println("[EXEC] Parsed " + argNames.size() + " arguments: " + argNames);

        // Create a new context for the function execution
        ExecutionContext functionContext = new ExecutionContext();

        // Map function parameters (x1, x2, ...) to actual argument values
        for (int i = 0; i < argNames.size(); i++) {
            String argName = argNames.get(i).trim();
            int argValue;

            // Check if argument is a nested function call like (Add,x1,x2)
            if (argName.startsWith("(") && argName.endsWith(")")) {
                // Recursively evaluate nested function call
                argValue = evaluateNestedFunction(argName, context);
            } else {
                // Simple variable reference
                argValue = context.getVariable(argName);
            }

            functionContext.setVariable("x" + (i + 1), argValue);
            System.out.println("[EXEC]   Param x" + (i + 1) + " = " + argValue);
        }

        // Execute the function's instructions
        // Note: Function's EXIT should only exit the function, not the whole program
        List<SInstruction> functionInstructions = function.getInstructions();
        while (!functionContext.isTerminated() && functionContext.getCurrentInstructionIndex() < functionInstructions.size()) {
            SInstruction funcInst = functionInstructions.get(functionContext.getCurrentInstructionIndex());

            // Check if this is a jump to EXIT - if so, break out of function execution
            if (funcInst.getName() == InstructionName.GOTO_LABEL && "EXIT".equals(funcInst.getArgument("gotoLabel"))) {
                System.out.println("[EXEC] Function " + functionName + " exiting via GOTO EXIT");
                break;
            }
            if (funcInst.getName() == InstructionName.JUMP_NOT_ZERO && "EXIT".equals(funcInst.getArgument("JNZLabel"))) {
                int value = functionContext.getVariable(funcInst.getVariable());
                if (value != 0) {
                    System.out.println("[EXEC] Function " + functionName + " exiting via JNZ to EXIT");
                    break;
                }
            }
            if (funcInst.getName() == InstructionName.JUMP_ZERO && "EXIT".equals(funcInst.getArgument("JZLabel"))) {
                int value = functionContext.getVariable(funcInst.getVariable());
                if (value == 0) {
                    System.out.println("[EXEC] Function " + functionName + " exiting via JZ to EXIT");
                    break;
                }
            }

            executeInstruction(funcInst, functionContext, functionInstructions);
        }

        // Get the result from the function's 'y' variable
        int result = functionContext.getVariable("y");
        System.out.println("[EXEC] QUOTE result: " + result);

        // Store result in output variable
        context.setVariable(outputVar, result);

        // Only advance instruction pointer if this is a top-level QUOTE call
        // Nested function calls should NOT advance the main program's instruction pointer
        if (advanceInstructionPointer) {
            context.nextInstruction();
        }
    }

    private List<String> parseQuoteArguments(String argumentsStr) {
        List<String> arguments = new ArrayList<>();
        if (argumentsStr == null || argumentsStr.trim().isEmpty()) {
            return arguments;
        }

        int depth = 0;
        StringBuilder current = new StringBuilder();

        for (char c : argumentsStr.toCharArray()) {
            if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                arguments.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            arguments.add(current.toString().trim());
        }

        return arguments;
    }

    private int evaluateNestedFunction(String expression, ExecutionContext context) {
        // Remove outer parentheses
        expression = expression.substring(1, expression.length() - 1).trim();

        // Find first comma to separate function name from arguments
        int firstComma = findFirstCommaInExpression(expression);

        String funcName;
        String funcArgs;

        if (firstComma == -1) {
            funcName = expression;
            funcArgs = "";
        } else {
            funcName = expression.substring(0, firstComma).trim();
            funcArgs = expression.substring(firstComma + 1).trim();
        }

        System.out.println("[EXEC] Evaluating nested function: " + funcName + "(" + funcArgs + ")");

        // Create a temporary variable to store the result
        String tempVar = "temp_" + System.nanoTime();

        // Create a QUOTE instruction
        SInstruction quoteInst = new SInstruction();
        quoteInst.setType(InstructionType.SYNTHETIC);
        quoteInst.setName(InstructionName.QUOTE);
        quoteInst.setVariable(tempVar);
        quoteInst.addArgument("functionName", funcName);
        quoteInst.addArgument("functionArguments", funcArgs);

        // Execute the QUOTE instruction WITHOUT advancing the instruction pointer
        // (nested function calls should not affect the main program's instruction pointer)
        executeQuote(quoteInst, context, false);

        // Get the result and clean up the temp variable
        int result = context.getVariable(tempVar);
        context.setVariable(tempVar, 0); // Clean up temp variable

        return result;
    }

    private int findFirstCommaInExpression(String str) {
        int depth = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                return i;
            }
        }
        return -1;
    }
}