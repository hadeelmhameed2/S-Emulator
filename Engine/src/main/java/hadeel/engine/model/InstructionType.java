package hadeel.engine.model;

public enum InstructionType {
    BASIC("B"),
    SYNTHETIC("S");
    
    private final String display;
    
    InstructionType(String display) {
        this.display = display;
    }
    
    public String getDisplay() {
        return display;
    }
    
    public static InstructionType fromString(String type) {
        if ("basic".equalsIgnoreCase(type)) {
            return BASIC;
        } else if ("synthetic".equalsIgnoreCase(type)) {
            return SYNTHETIC;
        }
        throw new IllegalArgumentException("Unknown instruction type: " + type);
    }
}