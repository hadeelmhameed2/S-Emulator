package hadeel.engine.execution;

import hadeel.engine.model.*;
import java.util.*;

public class ExpansionEngine {
    private static int labelCounter = 1;
    private static int variableCounter = 1;
    
    public static SProgram expand(SProgram program, int targetDegree) {
        if (program == null) {
            System.out.println("[ExpansionEngine] ERROR: Input program is null");
            return null;
        }

        if (targetDegree <= 0) {
            return copyProgram(program);
        }

        resetCounters();
        SProgram expandedProgram = copyProgram(program);

        for (int degree = 0; degree < targetDegree; degree++) {
            System.out.println("[ExpansionEngine] Expanding degree " + (degree + 1) + "/" + targetDegree);
            expandedProgram = expandOneDegree(expandedProgram);
            if (expandedProgram == null) {
                System.out.println("[ExpansionEngine] ERROR: expandOneDegree returned null at degree " + (degree + 1));
                return null;
            }
            System.out.println("[ExpansionEngine] After degree " + (degree + 1) + ": " + expandedProgram.getInstructions().size() + " instructions");
        }

        return expandedProgram;
    }
    
    private static SProgram expandOneDegree(SProgram program) {
        SProgram expanded = new SProgram(program.getName());
        expanded.setFunctions(program.getFunctions());

        for (SInstruction instruction : program.getInstructions()) {
            if (instruction.getType() == InstructionType.SYNTHETIC) {
                List<SInstruction> expandedInstructions = expandInstruction(instruction, program);

                if (!expandedInstructions.isEmpty() && instruction.getLabel() != null) {
                    expandedInstructions.get(0).setLabel(instruction.getLabel());
                }

                for (SInstruction exp : expandedInstructions) {
                    exp.setParent(instruction);
                    expanded.addInstruction(exp);
                }
            } else {
                expanded.addInstruction(instruction.copy());
            }
        }

        return expanded;
    }

    private static List<SInstruction> expandInstruction(SInstruction instruction, SProgram program) {
        List<SInstruction> expanded = new ArrayList<>();

        switch (instruction.getName()) {
            case ZERO_VARIABLE:
                expanded.addAll(expandZeroVariable(instruction));
                break;
            case GOTO_LABEL:
                expanded.addAll(expandGotoLabel(instruction));
                break;
            case ASSIGNMENT:
                expanded.addAll(expandAssignment(instruction));
                break;
            case CONSTANT_ASSIGNMENT:
                expanded.addAll(expandConstantAssignment(instruction));
                break;
            case JUMP_ZERO:
                expanded.addAll(expandJumpZero(instruction));
                break;
            case JUMP_EQUAL_CONSTANT:
                expanded.addAll(expandJumpEqualConstant(instruction));
                break;
            case JUMP_EQUAL_VARIABLE:
                expanded.addAll(expandJumpEqualVariable(instruction));
                break;
            default:
                // QUOTE and other unknown instructions are NOT expanded - they stay as-is
                // QUOTE will be handled at execution time, not expansion time
                expanded.add(instruction.copy());
        }

        return expanded;
    }
    
    private static List<SInstruction> expandZeroVariable(SInstruction instruction) {
        List<SInstruction> expanded = new ArrayList<>();
        String label = getNextLabel();
        
        SInstruction decrease = new SInstruction(InstructionType.BASIC, 
            InstructionName.DECREASE, instruction.getVariable());
        decrease.setLabel(label);
        
        SInstruction jump = new SInstruction(InstructionType.BASIC, 
            InstructionName.JUMP_NOT_ZERO, instruction.getVariable());
        jump.addArgument("JNZLabel", label);
        
        expanded.add(decrease);
        expanded.add(jump);
        
        return expanded;
    }
    
    private static List<SInstruction> expandGotoLabel(SInstruction instruction) {
        List<SInstruction> expanded = new ArrayList<>();
        String tempVar = getNextVariable();
        String targetLabel = instruction.getArgument("gotoLabel");
        
        SInstruction increase = new SInstruction(InstructionType.BASIC, 
            InstructionName.INCREASE, tempVar);
        
        SInstruction jump = new SInstruction(InstructionType.BASIC, 
            InstructionName.JUMP_NOT_ZERO, tempVar);
        jump.addArgument("JNZLabel", targetLabel);
        
        expanded.add(increase);
        expanded.add(jump);
        
        return expanded;
    }
    
    private static List<SInstruction> expandAssignment(SInstruction instruction) {
        List<SInstruction> expanded = new ArrayList<>();
        String sourceVar = instruction.getArgument("assignedVariable");
        String targetVar = instruction.getVariable();
        String tempVar = getNextVariable();
        String label1 = getNextLabel();
        String label2 = getNextLabel();
        String label3 = getNextLabel();
        String zeroLabel = getNextLabel();
        
        // Zero target variable using basic instructions only
        SInstruction zeroDecrease = new SInstruction(InstructionType.BASIC, 
            InstructionName.DECREASE, targetVar);
        zeroDecrease.setLabel(zeroLabel);
        expanded.add(zeroDecrease);
        
        SInstruction zeroJump = new SInstruction(InstructionType.BASIC, 
            InstructionName.JUMP_NOT_ZERO, targetVar);
        zeroJump.addArgument("JNZLabel", zeroLabel);
        expanded.add(zeroJump);
        
        SInstruction checkSource = new SInstruction(InstructionType.BASIC, 
            InstructionName.JUMP_NOT_ZERO, sourceVar);
        checkSource.addArgument("JNZLabel", label1);
        expanded.add(checkSource);
        
        // Goto end using basic instructions only
        String gotoTemp = getNextVariable();
        SInstruction gotoIncrease = new SInstruction(InstructionType.BASIC, 
            InstructionName.INCREASE, gotoTemp);
        expanded.add(gotoIncrease);
        
        SInstruction gotoJump = new SInstruction(InstructionType.BASIC, 
            InstructionName.JUMP_NOT_ZERO, gotoTemp);
        gotoJump.addArgument("JNZLabel", label3);
        expanded.add(gotoJump);
        
        SInstruction decreaseSource = new SInstruction(InstructionType.BASIC, 
            InstructionName.DECREASE, sourceVar);
        decreaseSource.setLabel(label1);
        expanded.add(decreaseSource);
        
        SInstruction increaseTemp = new SInstruction(InstructionType.BASIC, 
            InstructionName.INCREASE, tempVar);
        expanded.add(increaseTemp);
        
        SInstruction loopBack1 = new SInstruction(InstructionType.BASIC, 
            InstructionName.JUMP_NOT_ZERO, sourceVar);
        loopBack1.addArgument("JNZLabel", label1);
        expanded.add(loopBack1);
        
        SInstruction decreaseTemp = new SInstruction(InstructionType.BASIC, 
            InstructionName.DECREASE, tempVar);
        decreaseTemp.setLabel(label2);
        expanded.add(decreaseTemp);
        
        SInstruction increaseTarget = new SInstruction(InstructionType.BASIC, 
            InstructionName.INCREASE, targetVar);
        expanded.add(increaseTarget);
        
        SInstruction increaseSource2 = new SInstruction(InstructionType.BASIC, 
            InstructionName.INCREASE, sourceVar);
        expanded.add(increaseSource2);
        
        SInstruction loopBack2 = new SInstruction(InstructionType.BASIC, 
            InstructionName.JUMP_NOT_ZERO, tempVar);
        loopBack2.addArgument("JNZLabel", label2);
        expanded.add(loopBack2);
        
        SInstruction neutral = new SInstruction(InstructionType.BASIC, 
            InstructionName.NEUTRAL, targetVar);
        neutral.setLabel(label3);
        expanded.add(neutral);
        
        return expanded;
    }
    
    private static List<SInstruction> expandConstantAssignment(SInstruction instruction) {
        List<SInstruction> expanded = new ArrayList<>();
        String targetVar = instruction.getVariable();
        int constant = Integer.parseInt(instruction.getArgument("constantValue"));
        
        SInstruction zero = new SInstruction(InstructionType.SYNTHETIC, 
            InstructionName.ZERO_VARIABLE, targetVar);
        expanded.add(zero);
        
        for (int i = 0; i < constant; i++) {
            SInstruction increase = new SInstruction(InstructionType.BASIC, 
                InstructionName.INCREASE, targetVar);
            expanded.add(increase);
        }
        
        return expanded;
    }
    
    private static List<SInstruction> expandJumpZero(SInstruction instruction) {
        List<SInstruction> expanded = new ArrayList<>();
        String targetLabel = instruction.getArgument("JZLabel");
        String skipLabel = getNextLabel();
        
        SInstruction jumpNotZero = new SInstruction(InstructionType.BASIC, 
            InstructionName.JUMP_NOT_ZERO, instruction.getVariable());
        jumpNotZero.addArgument("JNZLabel", skipLabel);
        expanded.add(jumpNotZero);
        
        SInstruction gotoTarget = new SInstruction(InstructionType.SYNTHETIC, 
            InstructionName.GOTO_LABEL, instruction.getVariable());
        gotoTarget.addArgument("gotoLabel", targetLabel);
        expanded.add(gotoTarget);
        
        SInstruction neutral = new SInstruction(InstructionType.BASIC, 
            InstructionName.NEUTRAL, "y");
        neutral.setLabel(skipLabel);
        expanded.add(neutral);
        
        return expanded;
    }
    
    private static List<SInstruction> expandJumpEqualConstant(SInstruction instruction) {
        List<SInstruction> expanded = new ArrayList<>();
        String targetLabel = instruction.getArgument("JEConstantLabel");
        int constant = Integer.parseInt(instruction.getArgument("constantValue"));
        String tempVar = getNextVariable();
        String skipLabel = getNextLabel();
        
        SInstruction assignment = new SInstruction(InstructionType.SYNTHETIC, 
            InstructionName.ASSIGNMENT, tempVar);
        assignment.addArgument("assignedVariable", instruction.getVariable());
        expanded.add(assignment);
        
        SInstruction checkZero = new SInstruction(InstructionType.SYNTHETIC, 
            InstructionName.JUMP_ZERO, tempVar);
        checkZero.addArgument("JZLabel", skipLabel);
        expanded.add(checkZero);
        
        for (int i = 0; i < constant; i++) {
            SInstruction decrease = new SInstruction(InstructionType.BASIC, 
                InstructionName.DECREASE, tempVar);
            expanded.add(decrease);
        }
        
        SInstruction jumpNotZero = new SInstruction(InstructionType.BASIC, 
            InstructionName.JUMP_NOT_ZERO, tempVar);
        jumpNotZero.addArgument("JNZLabel", skipLabel);
        expanded.add(jumpNotZero);
        
        SInstruction gotoTarget = new SInstruction(InstructionType.SYNTHETIC, 
            InstructionName.GOTO_LABEL, tempVar);
        gotoTarget.addArgument("gotoLabel", targetLabel);
        expanded.add(gotoTarget);
        
        SInstruction neutral = new SInstruction(InstructionType.BASIC, 
            InstructionName.NEUTRAL, "y");
        neutral.setLabel(skipLabel);
        expanded.add(neutral);
        
        return expanded;
    }
    
    private static List<SInstruction> expandJumpEqualVariable(SInstruction instruction) {
        List<SInstruction> expanded = new ArrayList<>();
        String targetLabel = instruction.getArgument("JEVariableLabel");
        String var2 = instruction.getArgument("variableName");
        String temp1 = getNextVariable();
        String temp2 = getNextVariable();
        String label1 = getNextLabel();
        String label2 = getNextLabel();
        String label3 = getNextLabel();
        
        SInstruction assign1 = new SInstruction(InstructionType.SYNTHETIC, 
            InstructionName.ASSIGNMENT, temp1);
        assign1.addArgument("assignedVariable", instruction.getVariable());
        expanded.add(assign1);
        
        SInstruction assign2 = new SInstruction(InstructionType.SYNTHETIC, 
            InstructionName.ASSIGNMENT, temp2);
        assign2.addArgument("assignedVariable", var2);
        expanded.add(assign2);
        
        SInstruction checkTemp1 = new SInstruction(InstructionType.SYNTHETIC, 
            InstructionName.JUMP_ZERO, temp1);
        checkTemp1.addArgument("JZLabel", label2);
        checkTemp1.setLabel(label1);
        expanded.add(checkTemp1);
        
        SInstruction checkTemp2 = new SInstruction(InstructionType.SYNTHETIC, 
            InstructionName.JUMP_ZERO, temp2);
        checkTemp2.addArgument("JZLabel", label3);
        expanded.add(checkTemp2);
        
        SInstruction decreaseTemp1 = new SInstruction(InstructionType.BASIC, 
            InstructionName.DECREASE, temp1);
        expanded.add(decreaseTemp1);
        
        SInstruction decreaseTemp2 = new SInstruction(InstructionType.BASIC, 
            InstructionName.DECREASE, temp2);
        expanded.add(decreaseTemp2);
        
        SInstruction gotoLoop = new SInstruction(InstructionType.SYNTHETIC, 
            InstructionName.GOTO_LABEL, temp1);
        gotoLoop.addArgument("gotoLabel", label1);
        expanded.add(gotoLoop);
        
        SInstruction checkTemp2Zero = new SInstruction(InstructionType.SYNTHETIC, 
            InstructionName.JUMP_ZERO, temp2);
        checkTemp2Zero.addArgument("JZLabel", targetLabel);
        checkTemp2Zero.setLabel(label2);
        expanded.add(checkTemp2Zero);
        
        SInstruction neutral = new SInstruction(InstructionType.BASIC, 
            InstructionName.NEUTRAL, "y");
        neutral.setLabel(label3);
        expanded.add(neutral);
        
        return expanded;
    }
    
    private static SProgram copyProgram(SProgram program) {
        SProgram copy = new SProgram(program.getName());
        for (SInstruction instruction : program.getInstructions()) {
            copy.addInstruction(instruction.copy());
        }
        copy.setFunctions(program.getFunctions());
        return copy;
    }
    
    private static void resetCounters() {
        labelCounter = 1000;
        variableCounter = 1000;
    }
    
    private static String getNextLabel() {
        return "L" + (labelCounter++);
    }
    
    private static String getNextVariable() {
        return "z" + (variableCounter++);
    }

    private static List<SInstruction> expandQuote(SInstruction instruction, SProgram program) {
        List<SInstruction> expanded = new ArrayList<>();

        String functionName = instruction.getArgument("functionName");
        String functionArguments = instruction.getArgument("functionArguments");

        System.out.println("[expandQuote] Expanding QUOTE: function=" + functionName + ", args=" + functionArguments + ", outputVar=" + instruction.getVariable());

        if (functionName == null) {
            System.out.println("[expandQuote] ERROR: No function name");
            return expanded;
        }

        // Find the function in the program
        SFunction function = null;
        for (SFunction func : program.getFunctions()) {
            if (func.getName().equals(functionName)) {
                function = func;
                break;
            }
        }

        if (function == null) {
            System.out.println("[expandQuote] ERROR: Function not found: " + functionName);
            System.out.println("[expandQuote] Available functions: " + program.getFunctions().stream().map(f -> f.getName()).collect(java.util.stream.Collectors.joining(", ")));
            return expanded;
        }

        System.out.println("[expandQuote] Found function: " + functionName + " with " + function.getInstructions().size() + " instructions");

        // Parse function arguments - handle both simple variables and nested function calls
        List<String> arguments = parseArguments(functionArguments);
        System.out.println("[expandQuote] Parsed " + arguments.size() + " arguments: " + arguments);

        // Process each argument - if it's a function call, expand it first
        Map<String, String> parameterMapping = new HashMap<>();
        int argIndex = 1;

        for (String arg : arguments) {
            if (arg.startsWith("(") && arg.endsWith(")")) {
                // This is a nested function call - expand it to a temporary variable
                String tempVar = getNextVariable();
                List<SInstruction> nestedExpansion = expandFunctionExpression(arg, tempVar, program);
                expanded.addAll(nestedExpansion);
                parameterMapping.put("x" + argIndex, tempVar);
            } else {
                // Simple variable reference
                parameterMapping.put("x" + argIndex, arg);
            }
            argIndex++;
        }

        // Build a variable renaming map for all local variables in the function
        // This ensures that when we inline the function, its internal variables don't collide
        Map<String, String> variableRenaming = new HashMap<>();
        variableRenaming.putAll(parameterMapping); // x1->arg1, x2->arg2, etc.

        // Find all variables used in the function body (except parameters and y)
        Set<String> functionVariables = new HashSet<>();
        for (SInstruction funcInst : function.getInstructions()) {
            if (funcInst.getVariable() != null && !funcInst.getVariable().equals("y")) {
                String var = funcInst.getVariable();
                if (!var.startsWith("x") || !parameterMapping.containsKey(var)) {
                    functionVariables.add(var);
                }
            }
            // Also check variables in arguments
            for (String argValue : funcInst.getArguments().values()) {
                if (argValue != null && argValue.matches("z\\d+")) {
                    functionVariables.add(argValue);
                }
            }
        }

        // Assign unique temp variables for each function-local variable
        for (String var : functionVariables) {
            if (!variableRenaming.containsKey(var)) {
                variableRenaming.put(var, getNextVariable());
            }
        }

        System.out.println("[expandQuote] Variable renaming for " + functionName + ": " + variableRenaming);

        // Create a label renaming map to handle function-local labels
        // Each function's EXIT label should be renamed to a unique label that points to after the function
        Map<String, String> labelRenaming = new HashMap<>();
        String functionExitLabel = getNextLabel();
        labelRenaming.put("EXIT", functionExitLabel);

        // Also rename any other labels in the function to avoid collisions
        for (SInstruction funcInst : function.getInstructions()) {
            if (funcInst.getLabel() != null && !funcInst.getLabel().isEmpty()) {
                String label = funcInst.getLabel();
                if (!labelRenaming.containsKey(label)) {
                    labelRenaming.put(label, getNextLabel());
                }
            }
            // Check for labels in jump arguments
            for (String argValue : funcInst.getArguments().values()) {
                if (argValue != null && !argValue.isEmpty() && !labelRenaming.containsKey(argValue)) {
                    // This might be a label reference
                    if (argValue.matches("[A-Z][A-Z0-9_]*") || argValue.matches("L\\d+")) {
                        labelRenaming.put(argValue, getNextLabel());
                    }
                }
            }
        }

        System.out.println("[expandQuote] Label renaming for " + functionName + ": " + labelRenaming);

        // Inline the function's instructions with parameter and label substitution
        String outputVariable = instruction.getVariable();

        for (SInstruction funcInst : function.getInstructions()) {
            SInstruction inlined = funcInst.copy();

            // Replace function parameters with actual arguments
            if (inlined.getVariable() != null) {
                String var = inlined.getVariable();
                if (var.equals("y")) {
                    inlined.setVariable(outputVariable);
                } else if (variableRenaming.containsKey(var)) {
                    inlined.setVariable(variableRenaming.get(var));
                }
            }

            // Replace labels
            if (inlined.getLabel() != null && labelRenaming.containsKey(inlined.getLabel())) {
                inlined.setLabel(labelRenaming.get(inlined.getLabel()));
            }

            // Replace variables and labels in arguments
            Map<String, String> newArguments = new HashMap<>();
            for (Map.Entry<String, String> entry : inlined.getArguments().entrySet()) {
                String value = entry.getValue();
                String newValue = value;

                // First try variable renaming
                if (variableRenaming.containsKey(value)) {
                    newValue = variableRenaming.get(value);
                }
                // Then try label renaming (for jump targets)
                else if (labelRenaming.containsKey(value)) {
                    newValue = labelRenaming.get(value);
                }

                newArguments.put(entry.getKey(), newValue);
            }
            inlined.getArguments().clear();
            inlined.getArguments().putAll(newArguments);

            expanded.add(inlined);
        }

        // Add a NEUTRAL instruction with the function's EXIT label
        // This is where jumps to EXIT within the function will land
        SInstruction exitPoint = new SInstruction(InstructionType.BASIC,
            InstructionName.NEUTRAL, outputVariable);
        exitPoint.setLabel(functionExitLabel);
        expanded.add(exitPoint);

        return expanded;
    }

    private static List<String> parseArguments(String argumentsStr) {
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
                // Top-level comma - argument separator
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

    private static List<SInstruction> expandFunctionExpression(String expression, String outputVar, SProgram program) {
        // Remove outer parentheses
        expression = expression.substring(1, expression.length() - 1).trim();

        // Parse function name and arguments
        int firstComma = findFirstComma(expression);

        String funcName;
        String funcArgs;

        if (firstComma == -1) {
            // No comma found - function has zero arguments (e.g., CONST0)
            funcName = expression;
            funcArgs = "";
        } else {
            funcName = expression.substring(0, firstComma).trim();
            funcArgs = expression.substring(firstComma + 1).trim();
        }

        System.out.println("[expandFunctionExpression] Expanding nested function: " + funcName + " with args: '" + funcArgs + "' to var: " + outputVar);

        // Create a QUOTE instruction for this nested function call
        SInstruction quoteInst = new SInstruction();
        quoteInst.setType(InstructionType.SYNTHETIC);
        quoteInst.setName(InstructionName.QUOTE);
        quoteInst.setVariable(outputVar);
        quoteInst.addArgument("functionName", funcName);
        quoteInst.addArgument("functionArguments", funcArgs);

        // Recursively expand this QUOTE instruction
        return expandQuote(quoteInst, program);
    }

    private static int findFirstComma(String str) {
        int depth = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) return i;
        }
        return -1;
    }
}