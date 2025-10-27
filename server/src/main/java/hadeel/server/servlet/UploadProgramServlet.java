package hadeel.server.servlet;

import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import javax.servlet.http.*;
import javax.servlet.annotation.MultipartConfig;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@MultipartConfig
public class UploadProgramServlet extends HttpServlet {
    private ServerManager serverManager;

    @Override
    public void init() {
        serverManager = ServerManager.getInstance();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Get username from request parameter
            String username = req.getParameter("username");
            if (username == null || username.trim().isEmpty()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Username is required");
                return;
            }

            // Get XML content from request body
            String xmlContent = req.getReader().lines().collect(Collectors.joining("\n"));

            if (xmlContent == null || xmlContent.trim().isEmpty()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "XML content is required");
                return;
            }

            // Upload program through ServerManager
            ServerManager.UploadResult result = serverManager.uploadProgram(username.trim(), xmlContent);

            if (result.isSuccess()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", result.getMessage());
                JsonUtil.sendSuccess(resp, response);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, result.getMessage());
            }

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error uploading program: " + e.getMessage());
        }
    }
}
