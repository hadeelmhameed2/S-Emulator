package hadeel.server.model;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class User {
    private final String username;
    private final AtomicInteger credits;
    private final AtomicInteger creditsUsed;
    private final List<String> uploadedPrograms;
    private final List<String> contributedFunctions;
    private final List<ExecutionRecord> executionHistory;
    private final AtomicInteger executionCount;

    public User(String username) {
        this.username = username;
        this.credits = new AtomicInteger(1000); // Start with 1000 credits
        this.creditsUsed = new AtomicInteger(0);
        this.uploadedPrograms = Collections.synchronizedList(new ArrayList<>());
        this.contributedFunctions = Collections.synchronizedList(new ArrayList<>());
        this.executionHistory = Collections.synchronizedList(new ArrayList<>());
        this.executionCount = new AtomicInteger(0);
    }

    public String getUsername() {
        return username;
    }

    public int getCredits() {
        return credits.get();
    }

    public void addCredits(int amount) {
        credits.addAndGet(amount);
    }

    public boolean deductCredits(int amount) {
        int current;
        int updated;
        do {
            current = credits.get();
            if (current < amount) {
                return false;
            }
            updated = current - amount;
        } while (!credits.compareAndSet(current, updated));

        creditsUsed.addAndGet(amount);
        return true;
    }

    public int getCreditsUsed() {
        return creditsUsed.get();
    }

    public List<String> getUploadedPrograms() {
        return new ArrayList<>(uploadedPrograms);
    }

    public void addUploadedProgram(String programName) {
        uploadedPrograms.add(programName);
    }

    public List<String> getContributedFunctions() {
        return new ArrayList<>(contributedFunctions);
    }

    public void addContributedFunction(String functionName) {
        if (!contributedFunctions.contains(functionName)) {
            contributedFunctions.add(functionName);
        }
    }

    public List<ExecutionRecord> getExecutionHistory() {
        synchronized (executionHistory) {
            return new ArrayList<>(executionHistory);
        }
    }

    public void addExecutionRecord(ExecutionRecord record) {
        executionHistory.add(record);
        executionCount.incrementAndGet();
    }

    public int getExecutionCount() {
        return executionCount.get();
    }

    public int getProgramCount() {
        return uploadedPrograms.size();
    }

    public int getFunctionCount() {
        return contributedFunctions.size();
    }
}
