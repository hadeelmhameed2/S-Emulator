package hadeel.server.servlet;

import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class DebugStopServlet extends HttpServlet {
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

            // Remove debug session
            serverManager.removeDebugSession(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Debug session stopped");

            JsonUtil.sendSuccess(resp, response);

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error stopping debug session: " + e.getMessage());
        }
    }
}
