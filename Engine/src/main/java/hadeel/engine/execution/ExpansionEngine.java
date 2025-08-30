package hadeel.engine.execution;

import hadeel.engine.model.*;
import java.util.*;

public class ExpansionEngine {
    private static int labelCounter = 1;
    private static int variableCounter = 1;
    
    public static SProgram expand(SProgram program, int targetDegree) {
        if (targetDegree <= 0) {
            return copyProgram(program);
        }
        
        resetCounters();
        SProgram expandedProgram = copyProgram(program);
        
        for (int degree = 0; degree < targetDegree; degree++) {
            expandedProgram = expandOneDegree(expandedProgram);
        }
        
        return expandedProgram;
    }
    
    private static SProgram expandOneDegree(SProgram program) {
        SProgram expanded = new SProgram(program.getName());
        expanded.setFunctions(program.getFunctions());
        
        for (SInstruction instruction : program.getInstructions()) {
            if (instruction.getType() == InstructionType.SYNTHETIC) {
                List<SInstruction> expandedInstructions = expandInstruction(instruction);
                
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
    
    private static List<SInstruction> expandInstruction(SInstruction instruction) {
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
}