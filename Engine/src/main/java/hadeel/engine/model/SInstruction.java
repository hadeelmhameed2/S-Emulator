package hadeel.engine.model;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class SInstruction implements Serializable {
    private InstructionType type;
    private InstructionName name;
    private String variable;
    private String label;
    private Map<String, String> arguments;
    private SInstruction parent;
    private List<SInstruction> expandedInstructions;
    private int lineNumber;
    
    public SInstruction() {
        this.arguments = new HashMap<>();
        this.expandedInstructions = new ArrayList<>();
    }
    
    public SInstruction(InstructionType type, InstructionName name, String variable) {
        this();
        this.type = type;
        this.name = name;
        this.variable = variable;
    }
    
    public SInstruction copy() {
        SInstruction copy = new SInstruction();
        copy.type = this.type;
        copy.name = this.name;
        copy.variable = this.variable;
        copy.label = this.label;
        copy.arguments = new HashMap<>(this.arguments);
        copy.parent = this.parent;
        copy.lineNumber = this.lineNumber;
        return copy;
    }
    
    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();
        
        switch (name) {
            case INCREASE:
                sb.append(variable).append(" <- ").append(variable).append(" + 1");
                break;
            case DECREASE:
                sb.append(variable).append(" <- ").append(variable).append(" - 1");
                break;
            case JUMP_NOT_ZERO:
                sb.append("IF ").append(variable).append(" != 0 GOTO ")
                  .append(arguments.get("JNZLabel"));
                break;
            case NEUTRAL:
                sb.append(variable).append(" <- ").append(variable);
                break;
            case ZERO_VARIABLE:
                sb.append(variable).append(" <- 0");
                break;
            case GOTO_LABEL:
                sb.append("GOTO ").append(arguments.get("gotoLabel"));
                break;
            case ASSIGNMENT:
                sb.append(variable).append(" <- ").append(arguments.get("assignedVariable"));
                break;
            case CONSTANT_ASSIGNMENT:
                sb.append(variable).append(" <- ").append(arguments.get("constantValue"));
                break;
            case JUMP_ZERO:
                sb.append("IF ").append(variable).append(" = 0 GOTO ")
                  .append(arguments.get("JZLabel"));
                break;
            case JUMP_EQUAL_CONSTANT:
                sb.append("IF ").append(variable).append(" = ")
                  .append(arguments.get("constantValue")).append(" GOTO ")
                  .append(arguments.get("JEConstantLabel"));
                break;
            case JUMP_EQUAL_VARIABLE:
                sb.append("IF ").append(variable).append(" = ")
                  .append(arguments.get("variableName")).append(" GOTO ")
                  .append(arguments.get("JEVariableLabel"));
                break;
            case QUOTE:
                sb.append(variable).append(" <- (")
                  .append(arguments.get("functionName")).append(",")
                  .append(arguments.get("functionArguments")).append(")");
                break;
            case JUMP_EQUAL_FUNCTION:
                sb.append("IF ").append(variable).append(" = ")
                  .append(arguments.get("functionName")).append("(")
                  .append(arguments.get("functionArguments")).append(") GOTO ")
                  .append(arguments.get("JEFunctionLabel"));
                break;
            default:
                sb.append(name);
        }
        
        return sb.toString();
    }
    
    public String getFormattedDisplay(int number) {
        String labelStr = label != null ? 
            String.format("%-5s", label) : "     ";
        
        return String.format("#%-3d (%s) [%s] %s (%d)",
            number, type.getDisplay(), labelStr, 
            getDisplayString(), getCycles());
    }
    
    public int getCycles() {
        return name.getCycles();
    }
    
    public int getDegree() {
        if (type == InstructionType.BASIC) {
            return 0;
        }
        
        switch (name) {
            case ZERO_VARIABLE:
            case GOTO_LABEL:
                return 1;
            case ASSIGNMENT:
            case CONSTANT_ASSIGNMENT:
            case JUMP_ZERO:
                return 2;
            case JUMP_EQUAL_CONSTANT:
            case JUMP_EQUAL_VARIABLE:
                return 3;
            case QUOTE:
                return 4;
            case JUMP_EQUAL_FUNCTION:
                return 5;
            default:
                return 0;
        }
    }
    
    public InstructionType getType() {
        return type;
    }
    
    public void setType(InstructionType type) {
        this.type = type;
    }
    
    public InstructionName getName() {
        return name;
    }
    
    public void setName(InstructionName name) {
        this.name = name;
    }
    
    public String getVariable() {
        return variable;
    }
    
    public void setVariable(String variable) {
        this.variable = variable;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public Map<String, String> getArguments() {
        return arguments;
    }
    
    public void setArguments(Map<String, String> arguments) {
        this.arguments = arguments;
    }
    
    public void addArgument(String name, String value) {
        this.arguments.put(name, value);
    }
    
    public String getArgument(String name) {
        return arguments.get(name);
    }
    
    public SInstruction getParent() {
        return parent;
    }
    
    public void setParent(SInstruction parent) {
        this.parent = parent;
    }
    
    public List<SInstruction> getExpandedInstructions() {
        return expandedInstructions;
    }
    
    public void setExpandedInstructions(List<SInstruction> expandedInstructions) {
        this.expandedInstructions = expandedInstructions;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}