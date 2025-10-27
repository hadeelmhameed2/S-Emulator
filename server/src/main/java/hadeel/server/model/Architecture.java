package hadeel.server.model;

import hadeel.engine.model.InstructionName;
import java.util.Set;
import java.util.HashSet;

public enum Architecture {
    GENERATION_I(5, "I", Set.of(
        InstructionName.NEUTRAL,
        InstructionName.INCREASE,
        InstructionName.DECREASE,
        InstructionName.JUMP_NOT_ZERO
    )),
    GENERATION_II(100, "II", Set.of(
        InstructionName.NEUTRAL,
        InstructionName.INCREASE,
        InstructionName.DECREASE,
        InstructionName.JUMP_NOT_ZERO,
        InstructionName.ZERO_VARIABLE,
        InstructionName.CONSTANT_ASSIGNMENT,
        InstructionName.GOTO_LABEL
    )),
    GENERATION_III(500, "III", Set.of(
        InstructionName.NEUTRAL,
        InstructionName.INCREASE,
        InstructionName.DECREASE,
        InstructionName.JUMP_NOT_ZERO,
        InstructionName.ZERO_VARIABLE,
        InstructionName.CONSTANT_ASSIGNMENT,
        InstructionName.GOTO_LABEL,
        InstructionName.ASSIGNMENT,
        InstructionName.JUMP_ZERO,
        InstructionName.JUMP_EQUAL_CONSTANT,
        InstructionName.JUMP_EQUAL_VARIABLE
    )),
    GENERATION_IV(1000, "IV", Set.of(
        InstructionName.NEUTRAL,
        InstructionName.INCREASE,
        InstructionName.DECREASE,
        InstructionName.JUMP_NOT_ZERO,
        InstructionName.ZERO_VARIABLE,
        InstructionName.CONSTANT_ASSIGNMENT,
        InstructionName.GOTO_LABEL,
        InstructionName.ASSIGNMENT,
        InstructionName.JUMP_ZERO,
        InstructionName.JUMP_EQUAL_CONSTANT,
        InstructionName.JUMP_EQUAL_VARIABLE,
        InstructionName.QUOTE,
        InstructionName.JUMP_EQUAL_FUNCTION
    ));

    private final int cost;
    private final String displayName;
    private final Set<InstructionName> supportedInstructions;

    Architecture(int cost, String displayName, Set<InstructionName> supportedInstructions) {
        this.cost = cost;
        this.displayName = displayName;
        this.supportedInstructions = new HashSet<>(supportedInstructions);
    }

    public int getCost() {
        return cost;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<InstructionName> getSupportedInstructions() {
        return new HashSet<>(supportedInstructions);
    }

    public boolean supports(InstructionName instruction) {
        return supportedInstructions.contains(instruction);
    }

    public static Architecture fromString(String name) {
        for (Architecture arch : values()) {
            if (arch.displayName.equalsIgnoreCase(name) || arch.name().equalsIgnoreCase(name)) {
                return arch;
            }
        }
        throw new IllegalArgumentException("Unknown architecture: " + name);
    }
}
