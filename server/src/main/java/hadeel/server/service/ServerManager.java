package hadeel.server.service;

import hadeel.server.model.*;
import hadeel.engine.model.*;
import hadeel.engine.parser.XMLParser;
import hadeel.engine.parser.ParseResult;
import hadeel.engine.execution.ExecutionDebugger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerManager {
    private static ServerManager instance;

    private final Map<String, User> users;
    private final Map<String, ProgramInfo> programs;
    private final Map<String, FunctionInfo> functions;
    private final Map<String, ExecutionDebugger> activeDebugSessions;
    private final AtomicInteger executionIdCounter;

    private ServerManager() {
        this.users = new ConcurrentHashMap<>();
        this.programs = new ConcurrentHashMap<>();
        this.functions = new ConcurrentHashMap<>();
        this.activeDebugSessions = new ConcurrentHashMap<>();
        this.executionIdCounter = new AtomicInteger(0);
    }

    public static synchronized ServerManager getInstance() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }

    // User Management
    public synchronized User loginUser(String username) {
        if (users.containsKey(username)) {
            return null; // User already exists
        }
        User user = new User(username);
        users.put(username, user);
        return user;
    }

    public synchronized boolean logoutUser(String username) {
        return users.remove(username) != null;
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    // Program Management
    public synchronized UploadResult uploadProgram(String username, String xmlContent) {
        try {
            System.out.println("[ServerManager] Starting upload for user: " + username);
            // Parse the XML
            ParseResult parseResult = XMLParser.parseXMLString(xmlContent);
            if (!parseResult.isSuccess()) {
                System.out.println("[ServerManager] Parse failed: " + parseResult.getErrorMessage());
                return new UploadResult(false, parseResult.getErrorMessage());
            }

            SProgram program = parseResult.getProgram();
            String programName = program.getName();
            System.out.println("[ServerManager] Parsed program: " + programName + " with " + program.getInstructions().size() + " instructions and " + program.getFunctions().size() + " functions");

            // Validation 1: Check if program name already exists
            if (programs.containsKey(programName)) {
                return new UploadResult(false, "Program with name '" + programName + "' already exists");
            }

            // Validation 2: Check if all referenced functions exist
            Set<String> referencedFunctions = extractReferencedFunctions(program);
            for (String funcName : referencedFunctions) {
                if (!functions.containsKey(funcName) && !isProgramFunction(program, funcName)) {
                    return new UploadResult(false, "Program references undefined function: " + funcName);
                }
            }

            // Validation 3: Check if any function in this program already exists
            for (SFunction function : program.getFunctions()) {
                if (functions.containsKey(function.getName())) {
                    return new UploadResult(false, "Function '" + function.getName() + "' already exists in system");
                }
            }

            // All validations passed - add program and functions
            ProgramInfo programInfo = new ProgramInfo(program, username);
            programs.put(programName, programInfo);

            User user = users.get(username);
            System.out.println("[ServerManager] Looking for user: " + username + ", found: " + (user != null));
            if (user != null) {
                user.addUploadedProgram(programName);

                // Add functions from this program
                System.out.println("[ServerManager] Adding functions from program. Function count: " + program.getFunctions().size());
                for (SFunction function : program.getFunctions()) {
                    FunctionInfo functionInfo = new FunctionInfo(function, programName, username);
                    functions.put(function.getName(), functionInfo);
                    user.addContributedFunction(function.getName());
                    System.out.println("[ServerManager] Added function: " + function.getName() + " with " + function.getInstructions().size() + " instructions");
                }
            } else {
                System.out.println("[ServerManager] ERROR: User not found! Cannot add functions.");
            }

            System.out.println("[ServerManager] Upload complete. Total functions in repository: " + functions.size());
            return new UploadResult(true, "Program uploaded successfully");

        } catch (Exception e) {
            return new UploadResult(false, "Error parsing program: " + e.getMessage());
        }
    }

    private Set<String> extractReferencedFunctions(SProgram program) {
        Set<String> functions = new HashSet<>();
        for (SInstruction inst : program.getInstructions()) {
            if (inst.getName() == InstructionName.QUOTE) {
                String funcName = inst.getArgument("functionName");
                if (funcName != null) {
                    functions.add(funcName);
                }
            }
        }
        return functions;
    }

    private boolean isProgramFunction(SProgram program, String functionName) {
        for (SFunction func : program.getFunctions()) {
            if (func.getName().equals(functionName)) {
                return true;
            }
        }
        return false;
    }

    public ProgramInfo getProgram(String programName) {
        return programs.get(programName);
    }

    public List<ProgramInfo> getAllPrograms() {
        return new ArrayList<>(programs.values());
    }

    public List<FunctionInfo> getAllFunctions() {
        return new ArrayList<>(functions.values());
    }

    public FunctionInfo getFunction(String functionName) {
        return functions.get(functionName);
    }

    // Credit Management
    public boolean addCredits(String username, int amount) {
        User user = users.get(username);
        if (user == null) {
            return false;
        }
        user.addCredits(amount);
        return true;
    }

    public boolean deductCredits(String username, int amount) {
        User user = users.get(username);
        if (user == null) {
            return false;
        }
        return user.deductCredits(amount);
    }

    // Debug Session Management
    public String createDebugSession(ExecutionDebugger debugger, String username) {
        String sessionId = username + "_" + executionIdCounter.incrementAndGet();
        activeDebugSessions.put(sessionId, debugger);
        return sessionId;
    }

    public ExecutionDebugger getDebugSession(String sessionId) {
        return activeDebugSessions.get(sessionId);
    }

    public void removeDebugSession(String sessionId) {
        activeDebugSessions.remove(sessionId);
    }

    // Execution History
    public void addExecutionRecord(String username, ExecutionRecord record) {
        User user = users.get(username);
        if (user != null) {
            user.addExecutionRecord(record);

            // Update program statistics
            ProgramInfo programInfo = programs.get(record.getProgramName());
            if (programInfo != null) {
                programInfo.incrementExecutionCount();
                programInfo.addCreditsUsed(record.getCreditsUsed());
            }
        }
    }

    public List<ExecutionRecord> getUserExecutionHistory(String username) {
        User user = users.get(username);
        if (user == null) {
            return new ArrayList<>();
        }
        return user.getExecutionHistory();
    }

    // Inner class for upload results
    public static class UploadResult {
        private final boolean success;
        private final String message;

        public UploadResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
