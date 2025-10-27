package hadeel.server.servlet;

import hadeel.server.model.*;
import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import hadeel.engine.model.*;
import hadeel.engine.execution.ExecutionEngine;
import hadeel.engine.execution.ExpansionEngine;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class ExecuteServlet extends HttpServlet {
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

            // Expand program to requested degree
            SProgram expandedProgram = ExpansionEngine.expand(program, degree);

            // Validate architecture support
            for (SInstruction inst : expandedProgram.getInstructions()) {
                if (!architecture.supports(inst.getName())) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Program contains instructions not supported by architecture: " + inst.getName());
                    return;
                }
            }

            // Check credits (architecture cost + estimated cycles)
            int estimatedCost = architecture.getCost() + expandedProgram.getTotalCycles();
            if (user.getCredits() < estimatedCost) {
                Map<String, Object> errorResp = new HashMap<>();
                errorResp.put("error", "Insufficient credits");
                errorResp.put("required", estimatedCost);
                errorResp.put("available", user.getCredits());
                JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, errorResp);
                return;
            }

            // Deduct architecture cost upfront
            if (!serverManager.deductCredits(username, architecture.getCost())) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "Failed to deduct credits");
                return;
            }

            // Execute program
            ExecutionEngine engine = new ExecutionEngine();
            ExecutionResult result = engine.execute(expandedProgram, inputs != null ? inputs : new ArrayList<>(), degree);

            // Deduct cycle credits
            int cycleCredits = result.getCyclesConsumed();
            if (!serverManager.deductCredits(username, cycleCredits)) {
                // Ran out of credits during execution
                Map<String, Object> errorResp = new HashMap<>();
                errorResp.put("error", "Ran out of credits during execution");
                errorResp.put("partialResult", true);
                JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, errorResp);
                return;
            }

            // Record execution
            int totalCredits = architecture.getCost() + cycleCredits;
            ExecutionRecord record = new ExecutionRecord(
                user.getExecutionCount() + 1,
                programName,
                false,
                architecture,
                degree,
                inputs != null ? inputs : new ArrayList<>(),
                result.getOutputValue(),
                result.getCyclesConsumed(),
                totalCredits
            );
            serverManager.addExecutionRecord(username, record);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("output", result.getOutputValue());
            response.put("cycles", result.getCyclesConsumed());
            response.put("creditsUsed", totalCredits);
            response.put("remainingCredits", user.getCredits());
            response.put("variables", result.getFinalVariables());

            JsonUtil.sendSuccess(resp, response);

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error executing program: " + e.getMessage());
        }
    }
}
