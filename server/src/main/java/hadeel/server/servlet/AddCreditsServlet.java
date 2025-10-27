package hadeel.server.servlet;

import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AddCreditsServlet extends HttpServlet {
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
            Object amountObj = request.get("amount");

            if (username == null || username.trim().isEmpty()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Username is required");
                return;
            }

            if (amountObj == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Amount is required");
                return;
            }

            int amount;
            if (amountObj instanceof Number) {
                amount = ((Number) amountObj).intValue();
            } else {
                amount = Integer.parseInt(amountObj.toString());
            }

            if (amount <= 0) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Amount must be positive");
                return;
            }

            boolean success = serverManager.addCredits(username.trim(), amount);

            if (!success) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Credits added successfully");
            response.put("newBalance", serverManager.getUser(username).getCredits());

            JsonUtil.sendSuccess(resp, response);

        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid amount format");
        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error adding credits: " + e.getMessage());
        }
    }
}
