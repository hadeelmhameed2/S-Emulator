package hadeel.server.servlet;

import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import hadeel.engine.execution.ExecutionDebugger;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class DebugStepBackServlet extends HttpServlet {
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

            if (sessionId == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "SessionId is required");
                return;
            }

            ExecutionDebugger debugger = serverManager.getDebugSession(sessionId);
            if (debugger == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Debug session not found");
                return;
            }

            if (!debugger.canStepBack()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cannot step back further");
                return;
            }

            // Perform step back (no credit cost)
            debugger.stepBack();

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("currentLine", debugger.getCurrentLine());
            response.put("variables", debugger.getCurrentVariables());
            response.put("cycles", debugger.getCurrentCycles());
            response.put("finished", debugger.isFinished());
            response.put("canStepBack", debugger.canStepBack());

            JsonUtil.sendSuccess(resp, response);

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error performing step back: " + e.getMessage());
        }
    }
}
