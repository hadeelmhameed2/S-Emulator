package hadeel.server.model;

import java.util.List;

public class ExecutionRecord {
    private final int runNumber;
    private final String programName;
    private final boolean isFunction;
    private final Architecture architecture;
    private final int degree;
    private final List<Integer> inputs;
    private final int outputValue;
    private final int cyclesConsumed;
    private final int creditsUsed;
    private final long timestamp;

    public ExecutionRecord(int runNumber, String programName, boolean isFunction,
                          Architecture architecture, int degree, List<Integer> inputs,
                          int outputValue, int cyclesConsumed, int creditsUsed) {
        this.runNumber = runNumber;
        this.programName = programName;
        this.isFunction = isFunction;
        this.architecture = architecture;
        this.degree = degree;
        this.inputs = inputs;
        this.outputValue = outputValue;
        this.cyclesConsumed = cyclesConsumed;
        this.creditsUsed = creditsUsed;
        this.timestamp = System.currentTimeMillis();
    }

    public int getRunNumber() {
        return runNumber;
    }

    public String getProgramName() {
        return programName;
    }

    public boolean isFunction() {
        return isFunction;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public int getDegree() {
        return degree;
    }

    public List<Integer> getInputs() {
        return inputs;
    }

    public int getOutputValue() {
        return outputValue;
    }

    public int getCyclesConsumed() {
        return cyclesConsumed;
    }

    public int getCreditsUsed() {
        return creditsUsed;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
