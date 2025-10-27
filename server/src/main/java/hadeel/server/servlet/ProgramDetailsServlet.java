package hadeel.server.servlet;

import hadeel.engine.model.SInstruction;
import hadeel.server.model.ProgramInfo;
import hadeel.server.service.ServerManager;
import hadeel.server.util.JsonUtil;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class ProgramDetailsServlet extends HttpServlet {
    private ServerManager serverManager;

    @Override
    public void init() {
        serverManager = ServerManager.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Extract program name from path: /api/programs/ProgramName
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Program name required");
                return;
            }

            // Remove leading slash and URL-decode the program name
            String encodedName = pathInfo.substring(1);
            String programName = java.net.URLDecoder.decode(encodedName, "UTF-8");

            ProgramInfo programInfo = serverManager.getProgram(programName);
            if (programInfo == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Program not found: " + programName);
                return;
            }

            // Build response with program details
            Map<String, Object> programData = new HashMap<>();
            programData.put("name", programInfo.getName());
            programData.put("owner", programInfo.getOwnerUsername());
            programData.put("maxDegree", programInfo.getMaxDegree());

            // Convert instructions to JSON-friendly format
            List<Map<String, Object>> instructionsList = new ArrayList<>();
            List<SInstruction> instructions = programInfo.getProgram().getInstructions();

            for (int i = 0; i < instructions.size(); i++) {
                SInstruction inst = instructions.get(i);
                Map<String, Object> instData = new HashMap<>();

                instData.put("number", i + 1);
                instData.put("type", inst.getType().toString());
                instData.put("name", inst.getName().toString());
                instData.put("variable", inst.getVariable());
                instData.put("label", inst.getLabel());
                instData.put("cycles", inst.getCycles());
                instData.put("degree", inst.getDegree());

                // Convert arguments to JSON
                Map<String, String> arguments = new HashMap<>(inst.getArguments());
                instData.put("arguments", arguments);

                instructionsList.add(instData);
            }

            programData.put("instructions", instructionsList);

            JsonUtil.sendSuccess(resp, programData);

        } catch (Exception e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error fetching program details: " + e.getMessage());
        }
    }
}
