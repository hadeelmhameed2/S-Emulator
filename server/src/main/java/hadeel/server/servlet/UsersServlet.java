package hadeel.server.servlet;

import hadeel.server.model.User;
import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class UsersServlet extends HttpServlet {
    private ServerManager serverManager;

    @Override
    public void init() {
        serverManager = ServerManager.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<User> users = serverManager.getAllUsers();
            List<Map<String, Object>> usersList = new ArrayList<>();

            for (User user : users) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("username", user.getUsername());
                userInfo.put("programCount", user.getProgramCount());
                userInfo.put("functionCount", user.getFunctionCount());
                userInfo.put("credits", user.getCredits());
                userInfo.put("creditsUsed", user.getCreditsUsed());
                userInfo.put("executionCount", user.getExecutionCount());
                usersList.add(userInfo);
            }

            JsonUtil.sendSuccess(resp, usersList);

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error fetching users: " + e.getMessage());
        }
    }
}
