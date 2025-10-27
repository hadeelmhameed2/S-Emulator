package hadeel.server.servlet;

import hadeel.server.model.User;
import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import hadeel.engine.execution.ExecutionDebugger;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class DebugStepServlet extends HttpServlet {
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

            // Check if user has credits for one more cycle (worst case)
            if (user.getCredits() < 10) { // Safety buffer
                JsonUtil.sendError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "Insufficient credits");
                return;
            }

            int cyclesBefore = debugger.getCurrentCycles();

            // Perform step
            debugger.step();

            int cyclesAfter = debugger.getCurrentCycles();
            int cyclesUsed = cyclesAfter - cyclesBefore;

            // Deduct credits for this step
            if (cyclesUsed > 0) {
                if (!serverManager.deductCredits(username, cyclesUsed)) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "Ran out of credits");
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
            response.put("creditsUsed", cyclesUsed);
            response.put("remainingCredits", user.getCredits());
            response.put("canStepBack", debugger.canStepBack());

            JsonUtil.sendSuccess(resp, response);

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error performing debug step: " + e.getMessage());
        }
    }
}
