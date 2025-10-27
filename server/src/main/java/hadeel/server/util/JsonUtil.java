package hadeel.server.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JsonUtil {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public static void sendJsonResponse(HttpServletResponse response, int status, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(toJson(data));
    }

    public static void sendSuccess(HttpServletResponse response, Object data) throws IOException {
        sendJsonResponse(response, HttpServletResponse.SC_OK, data);
    }

    public static void sendError(HttpServletResponse response, int status, String message) throws IOException {
        ErrorResponse error = new ErrorResponse(message);
        sendJsonResponse(response, status, error);
    }

    public static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}
