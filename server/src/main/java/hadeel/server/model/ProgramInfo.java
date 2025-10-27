package hadeel.server.model;

import hadeel.engine.model.SProgram;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgramInfo {
    private final SProgram program;
    private final String ownerUsername;
    private final AtomicInteger executionCount;
    private final AtomicInteger totalCreditsUsed;

    public ProgramInfo(SProgram program, String ownerUsername) {
        this.program = program;
        this.ownerUsername = ownerUsername;
        this.executionCount = new AtomicInteger(0);
        this.totalCreditsUsed = new AtomicInteger(0);
    }

    public SProgram getProgram() {
        return program;
    }

    public String getName() {
        return program.getName();
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public int getInstructionCount() {
        return program.getInstructions().size();
    }

    public int getMaxDegree() {
        return program.getMaxDegree();
    }

    public int getExecutionCount() {
        return executionCount.get();
    }

    public void incrementExecutionCount() {
        executionCount.incrementAndGet();
    }

    public void addCreditsUsed(int credits) {
        totalCreditsUsed.addAndGet(credits);
    }

    public double getAverageCreditsUsed() {
        int execCount = executionCount.get();
        if (execCount == 0) {
            return 0.0;
        }
        return (double) totalCreditsUsed.get() / execCount;
    }
}
