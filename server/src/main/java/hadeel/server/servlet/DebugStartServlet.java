package hadeel.server.servlet;

import hadeel.server.model.*;
import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import hadeel.engine.model.*;
import hadeel.engine.execution.ExecutionDebugger;
import hadeel.engine.execution.ExpansionEngine;
import hadeel.engine.SEmulatorEngine;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class DebugStartServlet extends HttpServlet {
    private ServerManager serverManager;

    @Override
    public void init() {
        serverManager = ServerManager.getInstance();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String jsonBody = req.getReader().lines().reduce("", (acc, line) -> acc + line);
            Map<String, Object> request = JsonUtil.fromJson(jsonBody, Map.class);

            String username = (String) request.get("username");
            String programName = (String) request.get("programName");
            String archStr = (String) request.get("architecture");
            Object degreeObj = request.get("degree");

            // Convert inputs from List<Double> to List<Integer> (Gson parses numbers as Double)
            List<Integer> inputs = new ArrayList<>();
            Object inputsObj = request.get("inputs");
            if (inputsObj instanceof List) {
                for (Object obj : (List<?>) inputsObj) {
                    if (obj instanceof Number) {
                        inputs.add(((Number) obj).intValue());
                    }
                }
            }

            // Convert breakpoints from List<Double> to List<Integer>
            List<Integer> breakpoints = new ArrayList<>();
            Object breakpointsObj = request.get("breakpoints");
            if (breakpointsObj instanceof List) {
                for (Object obj : (List<?>) breakpointsObj) {
                    if (obj instanceof Number) {
                        breakpoints.add(((Number) obj).intValue());
                    }
                }
            }

            // Validation
            if (username == null || programName == null || archStr == null || degreeObj == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters");
                return;
            }

            User user = serverManager.getUser(username);
            if (user == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }

            ProgramInfo programInfo = serverManager.getProgram(programName);
            if (programInfo == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Program not found");
                return;
            }

            Architecture architecture = Architecture.fromString(archStr);
            int degree = ((Number) degreeObj).intValue();

            // Merge all available functions from repository into program
            SProgram program = programInfo.getProgram();
            List<FunctionInfo> allFunctions = serverManager.getAllFunctions();
            for (FunctionInfo funcInfo : allFunctions) {
                program.addFunction(funcInfo.getFunction());
            }

            // Expand program
            SProgram expandedProgram = ExpansionEngine.expand(program, degree);

            // Validate architecture support
            for (SInstruction inst : expandedProgram.getInstructions()) {
                if (!architecture.supports(inst.getName())) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Program contains unsupported instructions: " + inst.getName());
                    return;
                }
            }

            // Check and deduct architecture cost
            if (user.getCredits() < architecture.getCost()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "Insufficient credits");
                return;
            }

            if (!serverManager.deductCredits(username, architecture.getCost())) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "Failed to deduct credits");
                return;
            }

            // Create debugger
            SEmulatorEngine engine = new SEmulatorEngine();
            ExecutionDebugger debugger = new ExecutionDebugger(
                engine,
                expandedProgram,
                inputs != null ? inputs : new ArrayList<>(),
                degree
            );

            // Set breakpoints
            if (breakpoints != null && !breakpoints.isEmpty()) {
                debugger.setBreakpoints(new HashSet<>(breakpoints));
            }

            // Create session
            String sessionId = serverManager.createDebugSession(debugger, username);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("currentLine", debugger.getCurrentLine());
            response.put("variables", debugger.getCurrentVariables());
            response.put("cycles", debugger.getCurrentCycles());
            response.put("finished", debugger.isFinished());
            response.put("remainingCredits", user.getCredits());

            JsonUtil.sendSuccess(resp, response);

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error starting debug session: " + e.getMessage());
        }
    }
}
