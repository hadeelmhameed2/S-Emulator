package hadeel.server.servlet;

import hadeel.server.model.User;
import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import hadeel.engine.execution.ExecutionDebugger;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class DebugResumeServlet extends HttpServlet {
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

            String sessionId = (String) request.get("sessionId");
            String username = (String) request.get("username");

            if (sessionId == null || username == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "SessionId and username are required");
                return;
            }

            ExecutionDebugger debugger = serverManager.getDebugSession(sessionId);
            if (debugger == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Debug session not found");
                return;
            }

            User user = serverManager.getUser(username);
            if (user == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }

            int cyclesBefore = debugger.getCurrentCycles();
            int stepCount = 0;

            // Resume execution until breakpoint or finish
            while (!debugger.isFinished() && stepCount < 10000) { // Safety limit
                debugger.step();
                stepCount++;

                // Check if at breakpoint
                if (debugger.isAtBreakpoint()) {
                    break;
                }

                // Check if out of credits
                int currentCycles = debugger.getCurrentCycles();
                int cyclesUsed = currentCycles - cyclesBefore;
                if (cyclesUsed > user.getCredits()) {
                    // Out of credits
                    JsonUtil.sendError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "Ran out of credits during resume");
                    return;
                }
            }

            int cyclesAfter = debugger.getCurrentCycles();
            int totalCyclesUsed = cyclesAfter - cyclesBefore;

            // Deduct credits
            if (totalCyclesUsed > 0) {
                if (!serverManager.deductCredits(username, totalCyclesUsed)) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "Failed to deduct credits");
                    return;
                }
            }

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("currentLine", debugger.getCurrentLine());
            response.put("variables", debugger.getCurrentVariables());
            response.put("cycles", debugger.getCurrentCycles());
            response.put("finished", debugger.isFinished());
            response.put("stoppedAtBreakpoint", debugger.isAtBreakpoint());
            response.put("creditsUsed", totalCyclesUsed);
            response.put("remainingCredits", user.getCredits());
            response.put("stepCount", stepCount);

            JsonUtil.sendSuccess(resp, response);

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error resuming debug session: " + e.getMessage());
        }
    }
}
