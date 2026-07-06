package com.anti.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Minimal MCP Streamable HTTP JSON-RPC client for hosted MCP servers.
 */
@Slf4j
public class McpHttpClient implements AutoCloseable {

    private static final String SESSION_ID_HEADER = "mcp-session-id";

    private final String endpointUrl;
    private final Map<String, String> headers;
    private final String protocolVersion;
    private final int timeoutMs;
    private final AtomicLong requestIds = new AtomicLong(1);
    private final HttpClient client;

    private String sessionId;

    public McpHttpClient(String endpointUrl, Map<String, String> headers, String protocolVersion, int timeoutMs) {
        this.endpointUrl = endpointUrl == null ? "" : endpointUrl.trim();
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
        this.protocolVersion = hasText(protocolVersion) ? protocolVersion : "2025-06-18";
        this.timeoutMs = Math.max(1000, Math.min(timeoutMs <= 0 ? 20000 : timeoutMs, 60000));
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.timeoutMs))
                .build();
    }

    public void initialize() {
        JSONObject params = new JSONObject();
        params.set("protocolVersion", protocolVersion);
        params.set("capabilities", new JSONObject());

        JSONObject clientInfo = new JSONObject();
        clientInfo.set("name", "anti-fraud-platform");
        clientInfo.set("version", "1.0.0");
        params.set("clientInfo", clientInfo);

        request("initialize", params, timeoutMs);
        notifyInitialized();
    }

    public List<McpStdioClient.McpTool> listTools() {
        JSONObject result = request("tools/list", new JSONObject(), timeoutMs);
        JSONArray tools = result.getJSONArray("tools");
        if (tools == null || tools.isEmpty()) {
            return List.of();
        }

        List<McpStdioClient.McpTool> list = new ArrayList<>();
        for (int i = 0; i < tools.size(); i++) {
            JSONObject item = tools.getJSONObject(i);
            if (item == null) {
                continue;
            }
            McpStdioClient.McpTool tool = new McpStdioClient.McpTool();
            tool.setName(item.getStr("name"));
            tool.setDescription(item.getStr("description"));
            tool.setInputSchema(item.getJSONObject("inputSchema"));
            list.add(tool);
        }
        return list;
    }

    public McpStdioClient.McpToolResult callTool(String name, JSONObject arguments) {
        JSONObject params = new JSONObject();
        params.set("name", name);
        params.set("arguments", arguments == null ? new JSONObject() : arguments);

        JSONObject result = request("tools/call", params, timeoutMs);
        McpStdioClient.McpToolResult toolResult = new McpStdioClient.McpToolResult();
        toolResult.setRawResult(result);
        toolResult.setStructuredContent(result.getJSONObject("structuredContent"));
        toolResult.setError(Boolean.TRUE.equals(result.getBool("isError", false)));

        JSONArray content = result.getJSONArray("content");
        if (content != null) {
            List<String> texts = new ArrayList<>();
            for (int i = 0; i < content.size(); i++) {
                JSONObject item = content.getJSONObject(i);
                if (item != null && "text".equals(item.getStr("type")) && hasText(item.getStr("text"))) {
                    texts.add(item.getStr("text"));
                }
            }
            toolResult.setTextContents(texts);
        }
        return toolResult;
    }

    private JSONObject request(String method, JSONObject params, int requestTimeoutMs) {
        long id = requestIds.getAndIncrement();

        JSONObject request = new JSONObject();
        request.set("jsonrpc", "2.0");
        request.set("id", id);
        request.set("method", method);
        request.set("params", params == null ? new JSONObject() : params);

        JSONObject response = send(request, true, requestTimeoutMs);
        if (response == null) {
            throw new IllegalStateException("MCP HTTP request returned empty response: " + method);
        }
        JSONObject error = response.getJSONObject("error");
        if (error != null) {
            throw new IllegalStateException("MCP HTTP request failed: " + error.getStr("message"));
        }
        JSONObject result = response.getJSONObject("result");
        return result == null ? new JSONObject() : result;
    }

    private void notifyInitialized() {
        JSONObject notification = new JSONObject();
        notification.set("jsonrpc", "2.0");
        notification.set("method", "notifications/initialized");
        send(notification, false, timeoutMs);
    }

    private JSONObject send(JSONObject message, boolean expectResponse, int requestTimeoutMs) {
        if (!hasText(endpointUrl)) {
            throw new IllegalStateException("MCP HTTP endpoint URL is empty");
        }
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(Duration.ofMillis(Math.max(1000, requestTimeoutMs)))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(message.toString()));

            if (hasText(protocolVersion)) {
                builder.header("MCP-Protocol-Version", protocolVersion);
            }
            if (hasText(sessionId)) {
                builder.header("Mcp-Session-Id", sessionId);
            }
            for (Map.Entry<String, String> header : headers.entrySet()) {
                if (hasText(header.getKey()) && hasText(header.getValue())) {
                    builder.header(header.getKey(), header.getValue());
                }
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            captureSessionId(response);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("MCP HTTP status " + response.statusCode() + ": " + abbreviate(response.body(), 500));
            }
            if (!expectResponse) {
                return null;
            }
            return parseResponse(response);
        } catch (IOException e) {
            throw new IllegalStateException("MCP HTTP request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("MCP HTTP request interrupted", e);
        }
    }

    private void captureSessionId(HttpResponse<?> response) {
        response.headers().map().forEach((name, values) -> {
            if (SESSION_ID_HEADER.equals(name.toLowerCase(Locale.ROOT)) && values != null && !values.isEmpty()) {
                sessionId = values.get(0);
            }
        });
    }

    private JSONObject parseResponse(HttpResponse<String> response) {
        String body = response.body();
        if (!hasText(body)) {
            return null;
        }
        String contentType = response.headers()
                .firstValue("content-type")
                .orElse("")
                .toLowerCase(Locale.ROOT);
        if (contentType.contains("text/event-stream") || body.trim().startsWith("event:") || body.trim().startsWith("data:")) {
            return parseSseResponse(body);
        }
        return JSONUtil.parseObj(body);
    }

    private JSONObject parseSseResponse(String body) {
        JSONObject lastJson = null;
        StringBuilder data = new StringBuilder();
        String[] lines = body.split("\\R");
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                lastJson = parseSseData(data, lastJson);
                data.setLength(0);
                continue;
            }
            if (line.startsWith("data:")) {
                if (data.length() > 0) {
                    data.append('\n');
                }
                data.append(line.substring("data:".length()).trim());
            }
        }
        return parseSseData(data, lastJson);
    }

    private JSONObject parseSseData(StringBuilder data, JSONObject fallback) {
        if (data == null || data.length() == 0) {
            return fallback;
        }
        String payload = data.toString().trim();
        if (!hasText(payload) || "[DONE]".equals(payload)) {
            return fallback;
        }
        try {
            return JSONUtil.parseObj(payload);
        } catch (Exception e) {
            log.debug("Ignore invalid MCP SSE payload");
            return fallback;
        }
    }

    @Override
    public void close() {
        // HttpClient has no close hook on Java 17.
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
