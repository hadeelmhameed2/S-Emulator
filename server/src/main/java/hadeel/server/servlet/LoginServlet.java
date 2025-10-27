package hadeel.server.servlet;

import hadeel.server.model.User;
import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginServlet extends HttpServlet {
    private ServerManager serverManager;

    @Override
    public void init() {
        serverManager = ServerManager.getInstance();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String jsonBody = req.getReader().lines().reduce("", (acc, line) -> acc + line);
            Map<String, String> request = JsonUtil.fromJson(jsonBody, Map.class);

            String username = request.get("username");
            if (username == null || username.trim().isEmpty()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Username is required");
                return;
            }

            User user = serverManager.loginUser(username.trim());
            if (user == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "Username already exists");
                return;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("username", user.getUsername());
            response.put("credits", user.getCredits());
            response.put("message", "Login successful");

            JsonUtil.sendSuccess(resp, response);

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error processing login: " + e.getMessage());
        }
    }
}
