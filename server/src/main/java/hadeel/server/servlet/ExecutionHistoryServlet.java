package hadeel.server.servlet;

import hadeel.server.model.ExecutionRecord;
import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class ExecutionHistoryServlet extends HttpServlet {
    private ServerManager serverManager;

    @Override
    public void init() {
        serverManager = ServerManager.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String username = req.getParameter("username");
            if (username == null || username.trim().isEmpty()) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Username is required");
                return;
            }

            List<ExecutionRecord> history = serverManager.getUserExecutionHistory(username.trim());
            List<Map<String, Object>> historyList = new ArrayList<>();

            for (ExecutionRecord record : history) {
                Map<String, Object> recordData = new HashMap<>();
                recordData.put("runNumber", record.getRunNumber());
                recordData.put("programName", record.getProgramName());
                recordData.put("isFunction", record.isFunction());
                recordData.put("architecture", record.getArchitecture().getDisplayName());
                recordData.put("degree", record.getDegree());
                recordData.put("inputs", record.getInputs());
                recordData.put("output", record.getOutputValue());
                recordData.put("cycles", record.getCyclesConsumed());
                recordData.put("creditsUsed", record.getCreditsUsed());
                recordData.put("timestamp", record.getTimestamp());
                historyList.add(recordData);
            }

            JsonUtil.sendSuccess(resp, historyList);

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error fetching execution history: " + e.getMessage());
        }
    }
}
