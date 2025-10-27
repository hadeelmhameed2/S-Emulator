package hadeel.server.servlet;

import hadeel.server.model.FunctionInfo;
import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class FunctionsServlet extends HttpServlet {
    private ServerManager serverManager;

    @Override
    public void init() {
        serverManager = ServerManager.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<FunctionInfo> functions = serverManager.getAllFunctions();
            List<Map<String, Object>> functionsList = new ArrayList<>();

            for (FunctionInfo functionInfo : functions) {
                Map<String, Object> functionData = new HashMap<>();
                functionData.put("name", functionInfo.getName());
                functionData.put("parentProgram", functionInfo.getParentProgramName());
                functionData.put("owner", functionInfo.getOwnerUsername());
                functionData.put("instructionCount", functionInfo.getInstructionCount());
                functionData.put("maxDegree", functionInfo.getMaxDegree());
                functionsList.add(functionData);
            }

            JsonUtil.sendSuccess(resp, functionsList);

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error fetching functions: " + e.getMessage());
        }
    }
}
