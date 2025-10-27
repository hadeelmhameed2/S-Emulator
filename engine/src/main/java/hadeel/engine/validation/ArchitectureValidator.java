package hadeel.engine.validation;

import hadeel.engine.model.*;
import java.util.*;

public class ArchitectureValidator {

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> unsupportedInstructions;

        public ValidationResult(boolean valid, List<String> unsupportedInstructions) {
            this.valid = valid;
            this.unsupportedInstructions = unsupportedInstructions;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getUnsupportedInstructions() {
            return unsupportedInstructions;
        }
    }

    public static ValidationResult validate(SProgram program, Set<InstructionName> supportedInstructions) {
        List<String> unsupported = new ArrayList<>();

        for (SInstruction inst : program.getInstructions()) {
            if (!supportedInstructions.contains(inst.getName())) {
                unsupported.add(inst.getName().toString() + " at line " + inst.getLineNumber());
            }
        }

        return new ValidationResult(unsupported.isEmpty(), unsupported);
    }

    public static Map<String, Integer> countInstructionsByArchitecture(
            SProgram program,
            Map<String, Set<InstructionName>> architectures) {

        Map<String, Integer> counts = new HashMap<>();

        for (Map.Entry<String, Set<InstructionName>> entry : architectures.entrySet()) {
            String archName = entry.getKey();
            Set<InstructionName> supported = entry.getValue();

            int count = 0;
            for (SInstruction inst : program.getInstructions()) {
                if (supported.contains(inst.getName())) {
                    count++;
                }
            }
            counts.put(archName, count);
        }

        return counts;
    }

    public static int getMinimumArchitectureGeneration(SProgram program) {
        // Returns minimum generation (1-4) needed to run the program
        Set<InstructionName> usedInstructions = new HashSet<>();
        for (SInstruction inst : program.getInstructions()) {
            usedInstructions.add(inst.getName());
        }

        // Check Gen I (basic only)
        if (isGenICompatible(usedInstructions)) {
            return 1;
        }

        // Check Gen II
        if (isGenIICompatible(usedInstructions)) {
            return 2;
        }

        // Check Gen III
        if (isGenIIICompatible(usedInstructions)) {
            return 3;
        }

        // Must be Gen IV
        return 4;
    }

    private static boolean isGenICompatible(Set<InstructionName> instructions) {
        Set<InstructionName> genI = Set.of(
            InstructionName.NEUTRAL,
            InstructionName.INCREASE,
            InstructionName.DECREASE,
            InstructionName.JUMP_NOT_ZERO
        );
        return genI.containsAll(instructions);
    }

    private static boolean isGenIICompatible(Set<InstructionName> instructions) {
        Set<InstructionName> genII = new HashSet<>(Set.of(
            InstructionName.NEUTRAL,
            InstructionName.INCREASE,
            InstructionName.DECREASE,
            InstructionName.JUMP_NOT_ZERO,
            InstructionName.ZERO_VARIABLE,
            InstructionName.CONSTANT_ASSIGNMENT,
            InstructionName.GOTO_LABEL
        ));
        return genII.containsAll(instructions);
    }

    private static boolean isGenIIICompatible(Set<InstructionName> instructions) {
        Set<InstructionName> genIII = new HashSet<>(Set.of(
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
        ));
        return genIII.containsAll(instructions);
    }
}
