package zaga.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import zaga.utils.HwidUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ApiClient {
    private static final String BASE_URL = "https://zagadlc.zagir4ik985.workers.dev";
    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static JsonObject request(String path, String method, String jsonBody, String authHeader) throws Exception {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + path))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "zagaDLC-Loader/1.0");
                if (authHeader != null) {
                    builder.header("Authorization", authHeader);
                }
                if ("POST".equals(method)) {
                    builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody != null ? jsonBody : "{}"));
                } else {
                    builder.GET();
                }

                HttpResponse<String> resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                String body = resp.body() != null ? resp.body() : "";
                int code = resp.statusCode();

                if (code < 200 || code >= 300) {
                    String msg = "HTTP " + code;
                    if (!body.isBlank()) {
                        try {
                            JsonObject parsed = JsonParser.parseString(body).getAsJsonObject();
                            if (parsed.has("error")) msg = parsed.get("error").getAsString();
                        } catch (Exception e) {
                            msg = "HTTP " + code + ": " + body;
                        }
                    }
                    JsonObject err = new JsonObject();
                    err.addProperty("error", msg);
                    return err;
                }
                if (body.isBlank()) {
                    JsonObject err = new JsonObject();
                    err.addProperty("error", "Empty server response");
                    return err;
                }
                return JsonParser.parseString(body).getAsJsonObject();
            } catch (Exception e) {
                if (attempt == maxRetries) throw e;
                try { Thread.sleep(1000L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw ie; }
            }
        }
        throw new RuntimeException("Unreachable");
    }

    public static JsonObject login(String login, String password) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("login", login);
        body.addProperty("password", password);
        body.addProperty("hwid", HwidUtil.getHwid());
        return request("/api/login", "POST", gson.toJson(body), null);
    }

    public static JsonObject register(String login, String password) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("login", login);
        body.addProperty("password", password);
        body.addProperty("hwid", HwidUtil.getHwid());
        return request("/api/register", "POST", gson.toJson(body), null);
    }

    public static JsonObject userInfo(String token) throws Exception {
        return request("/api/user/info", "GET", null, "Bearer " + token);
    }

    public static String fetchKey(String token) throws Exception {
        JsonObject resp = request("/api/key", "GET", null, "Bearer " + token);
        if (resp.has("key")) {
            return resp.get("key").getAsString();
        }
        throw new RuntimeException("Failed to fetch key: " + resp);
    }
}
