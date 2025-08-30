package hadeel.engine.model;

public enum InstructionName {
    NEUTRAL(InstructionType.BASIC, 0),
    INCREASE(InstructionType.BASIC, 1),
    DECREASE(InstructionType.BASIC, 1),
    JUMP_NOT_ZERO(InstructionType.BASIC, 2),
    ZERO_VARIABLE(InstructionType.SYNTHETIC, 1),
    GOTO_LABEL(InstructionType.SYNTHETIC, 1),
    ASSIGNMENT(InstructionType.SYNTHETIC, 4),
    CONSTANT_ASSIGNMENT(InstructionType.SYNTHETIC, 2),
    JUMP_ZERO(InstructionType.SYNTHETIC, 2),
    JUMP_EQUAL_CONSTANT(InstructionType.SYNTHETIC, 2),
    JUMP_EQUAL_VARIABLE(InstructionType.SYNTHETIC, 2),
    QUOTE(InstructionType.SYNTHETIC, 5),
    JUMP_EQUAL_FUNCTION(InstructionType.SYNTHETIC, 6);
    
    private final InstructionType type;
    private final int cycles;
    
    InstructionName(InstructionType type, int cycles) {
        this.type = type;
        this.cycles = cycles;
    }
    
    public InstructionType getType() {
        return type;
    }
    
    public int getCycles() {
        return cycles;
    }
    
    public static InstructionName fromString(String name) {
        try {
            return InstructionName.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown instruction name: " + name);
        }
    }
}