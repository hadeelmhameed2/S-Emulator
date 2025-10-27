package hadeel.server.servlet;

import hadeel.server.model.ProgramInfo;
import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class ProgramsServlet extends HttpServlet {
    private ServerManager serverManager;

    @Override
    public void init() {
        serverManager = ServerManager.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<ProgramInfo> programs = serverManager.getAllPrograms();
            List<Map<String, Object>> programsList = new ArrayList<>();

            for (ProgramInfo programInfo : programs) {
                Map<String, Object> programData = new HashMap<>();
                programData.put("name", programInfo.getName());
                programData.put("owner", programInfo.getOwnerUsername());
                programData.put("instructionCount", programInfo.getInstructionCount());
                programData.put("maxDegree", programInfo.getMaxDegree());
                programData.put("executionCount", programInfo.getExecutionCount());
                programData.put("averageCredits", programInfo.getAverageCreditsUsed());
                programsList.add(programData);
            }

            JsonUtil.sendSuccess(resp, programsList);

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error fetching programs: " + e.getMessage());
        }
    }
}
