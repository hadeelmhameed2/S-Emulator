package hadeel.server.model;

import hadeel.engine.model.SFunction;

public class FunctionInfo {
    private final SFunction function;
    private final String parentProgramName;
    private final String ownerUsername;

    public FunctionInfo(SFunction function, String parentProgramName, String ownerUsername) {
        this.function = function;
        this.parentProgramName = parentProgramName;
        this.ownerUsername = ownerUsername;
    }

    public SFunction getFunction() {
        return function;
    }

    public String getName() {
        return function.getName();
    }

    public String getParentProgramName() {
        return parentProgramName;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public int getInstructionCount() {
        return function.getInstructions().size();
    }

    public int getMaxDegree() {
        return function.getMaxDegree();
    }
}
