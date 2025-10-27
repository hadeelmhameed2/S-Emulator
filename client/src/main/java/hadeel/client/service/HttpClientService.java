package hadeel.client.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class HttpClientService {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Gson gson;

    public HttpClientService(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
    }

    // User operations
    public Response<Map<String, Object>> login(String username) {
        Map<String, String> body = Map.of("username", username);
        return post("/api/login", body, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public Response<Map<String, Object>> logout(String username) {
        Map<String, String> body = Map.of("username", username);
        return post("/api/logout", body, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public Response<List<Map<String, Object>>> getUsers() {
        return get("/api/users", new TypeToken<List<Map<String, Object>>>(){}.getType());
    }

    public Response<Map<String, Object>> addCredits(String username, int amount) {
        Map<String, Object> body = Map.of("username", username, "amount", amount);
        return post("/api/credits/add", body, new TypeToken<Map<String, Object>>(){}.getType());
    }

    // Program operations
    public Response<Map<String, Object>> uploadProgram(String username, String xmlContent) {
        String url = baseUrl + "/api/programs/upload?username=" + username;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(xmlContent))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> data = gson.fromJson(response.body(),
                    new TypeToken<Map<String, Object>>(){}.getType());
                return new Response<>(true, data, null);
            } else {
                Map<String, Object> error = gson.fromJson(response.body(),
                    new TypeToken<Map<String, Object>>(){}.getType());
                return new Response<>(false, null, (String) error.get("error"));
            }
        } catch (Exception e) {
            return new Response<>(false, null, e.getMessage());
        }
    }

    public Response<List<Map<String, Object>>> getPrograms() {
        return get("/api/programs", new TypeToken<List<Map<String, Object>>>(){}.getType());
    }

    public Response<Map<String, Object>> getProgramDetails(String programName) {
        try {
            String encodedName = java.net.URLEncoder.encode(programName, "UTF-8");
            return get("/api/programs/details/" + encodedName, new TypeToken<Map<String, Object>>(){}.getType());
        } catch (java.io.UnsupportedEncodingException e) {
            return new Response<>(false, null, "Failed to encode program name: " + e.getMessage());
        }
    }

    public Response<List<Map<String, Object>>> getFunctions() {
        return get("/api/functions", new TypeToken<List<Map<String, Object>>>(){}.getType());
    }

    // Execution operations
    public Response<Map<String, Object>> execute(Map<String, Object> executeRequest) {
        return post("/api/execute", executeRequest, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public Response<Map<String, Object>> startDebug(Map<String, Object> debugRequest) {
        return post("/api/debug/start", debugRequest, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public Response<Map<String, Object>> debugStep(String sessionId, String username) {
        Map<String, String> body = Map.of("sessionId", sessionId, "username", username);
        return post("/api/debug/step", body, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public Response<Map<String, Object>> debugStepBack(String sessionId) {
        Map<String, String> body = Map.of("sessionId", sessionId);
        return post("/api/debug/stepback", body, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public Response<Map<String, Object>> debugResume(String sessionId, String username) {
        Map<String, String> body = Map.of("sessionId", sessionId, "username", username);
        return post("/api/debug/resume", body, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public Response<Map<String, Object>> debugStop(String sessionId) {
        Map<String, String> body = Map.of("sessionId", sessionId);
        return post("/api/debug/stop", body, new TypeToken<Map<String, Object>>(){}.getType());
    }

    public Response<List<Map<String, Object>>> getExecutionHistory(String username) {
        return get("/api/history?username=" + username, new TypeToken<List<Map<String, Object>>>(){}.getType());
    }

    // Generic HTTP methods
    private <T> Response<T> get(String endpoint, java.lang.reflect.Type typeOfT) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                T data = gson.fromJson(response.body(), typeOfT);
                return new Response<>(true, data, null);
            } else {
                Map<String, Object> error = gson.fromJson(response.body(),
                    new TypeToken<Map<String, Object>>(){}.getType());
                return new Response<>(false, null, (String) error.get("error"));
            }
        } catch (Exception e) {
            return new Response<>(false, null, e.getMessage());
        }
    }

    private <T> Response<T> post(String endpoint, Object body, java.lang.reflect.Type typeOfT) {
        try {
            String jsonBody = gson.toJson(body);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                T data = gson.fromJson(response.body(), typeOfT);
                return new Response<>(true, data, null);
            } else {
                Map<String, Object> error = gson.fromJson(response.body(),
                    new TypeToken<Map<String, Object>>(){}.getType());
                return new Response<>(false, null, (String) error.get("error"));
            }
        } catch (Exception e) {
            return new Response<>(false, null, e.getMessage());
        }
    }

    // Response wrapper class
    public static class Response<T> {
        private final boolean success;
        private final T data;
        private final String error;

        public Response(boolean success, T data, String error) {
            this.success = success;
            this.data = data;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public T getData() {
            return data;
        }

        public String getError() {
            return error;
        }
    }
}
