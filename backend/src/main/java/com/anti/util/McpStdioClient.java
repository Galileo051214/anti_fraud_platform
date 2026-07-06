package com.anti.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Minimal MCP stdio JSON-RPC client for local npm MCP servers.
 */
@Slf4j
public class McpStdioClient implements AutoCloseable {

    private final List<String> command;
    private final Map<String, String> environment;
    private final String protocolVersion;
    private final int timeoutMs;
    private final AtomicLong requestIds = new AtomicLong(1);
    private final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();

    private Process process;
    private BufferedWriter writer;
    private Thread stdoutReader;
    private Thread stderrReader;

    public McpStdioClient(List<String> command, Map<String, String> environment, String protocolVersion, int timeoutMs) {
        this.command = command == null ? List.of() : new ArrayList<>(command);
        this.environment = environment == null ? Map.of() : Map.copyOf(environment);
        this.protocolVersion = hasText(protocolVersion) ? protocolVersion : "2025-06-18";
        this.timeoutMs = Math.max(1000, Math.min(timeoutMs <= 0 ? 20000 : timeoutMs, 60000));
    }

    public void initialize() {
        startIfNecessary();

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

    public List<McpTool> listTools() {
        JSONObject result = request("tools/list", new JSONObject(), timeoutMs);
        JSONArray tools = result.getJSONArray("tools");
        if (tools == null || tools.isEmpty()) {
            return List.of();
        }

        List<McpTool> list = new ArrayList<>();
        for (int i = 0; i < tools.size(); i++) {
            JSONObject item = tools.getJSONObject(i);
            if (item == null) {
                continue;
            }
            McpTool tool = new McpTool();
            tool.setName(item.getStr("name"));
            tool.setDescription(item.getStr("description"));
            tool.setInputSchema(item.getJSONObject("inputSchema"));
            list.add(tool);
        }
        return list;
    }

    public McpToolResult callTool(String name, JSONObject arguments) {
        JSONObject params = new JSONObject();
        params.set("name", name);
        params.set("arguments", arguments == null ? new JSONObject() : arguments);

        JSONObject result = request("tools/call", params, timeoutMs);
        McpToolResult toolResult = new McpToolResult();
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
        startIfNecessary();
        long id = requestIds.getAndIncrement();

        JSONObject request = new JSONObject();
        request.set("jsonrpc", "2.0");
        request.set("id", id);
        request.set("method", method);
        request.set("params", params == null ? new JSONObject() : params);
        send(request);

        long deadline = System.currentTimeMillis() + Math.max(1000, requestTimeoutMs);
        while (System.currentTimeMillis() < deadline) {
            long waitMs = Math.max(1, deadline - System.currentTimeMillis());
            JSONObject response = pollMessage(waitMs);
            if (response == null) {
                continue;
            }
            if (!response.containsKey("id") || !String.valueOf(id).equals(String.valueOf(response.get("id")))) {
                continue;
            }
            JSONObject error = response.getJSONObject("error");
            if (error != null) {
                throw new IllegalStateException("MCP request failed: " + error.getStr("message"));
            }
            return response.getJSONObject("result");
        }
        throw new IllegalStateException("MCP request timeout: " + method);
    }

    private JSONObject pollMessage(long waitMs) {
        try {
            return messages.poll(waitMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("MCP request interrupted", e);
        }
    }

    private void notifyInitialized() {
        JSONObject notification = new JSONObject();
        notification.set("jsonrpc", "2.0");
        notification.set("method", "notifications/initialized");
        send(notification);
    }

    private synchronized void send(JSONObject message) {
        try {
            writer.write(message.toString());
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Write MCP message failed", e);
        }
    }

    private synchronized void startIfNecessary() {
        if (process != null && process.isAlive()) {
            return;
        }
        if (command.isEmpty()) {
            throw new IllegalStateException("MCP command is empty");
        }
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.environment().putAll(environment);
            process = processBuilder.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            startReaders();
        } catch (IOException e) {
            throw new IllegalStateException("Start MCP server failed", e);
        }
    }

    private void startReaders() {
        stdoutReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!hasText(line)) {
                        continue;
                    }
                    try {
                        messages.offer(JSONUtil.parseObj(line));
                    } catch (Exception e) {
                        log.debug("Ignore non-json MCP stdout line, command={}", command);
                    }
                }
            } catch (IOException e) {
                log.debug("MCP stdout reader stopped, command={}", command);
            }
        }, "mcp-stdio-stdout");
        stdoutReader.setDaemon(true);
        stdoutReader.start();

        stderrReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (hasText(line)) {
                        log.debug("MCP stderr: {}", line);
                    }
                }
            } catch (IOException e) {
                log.debug("MCP stderr reader stopped, command={}", command);
            }
        }, "mcp-stdio-stderr");
        stderrReader.setDaemon(true);
        stderrReader.start();
    }

    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Data
    public static class McpTool {
        private String name;
        private String description;
        private JSONObject inputSchema;
    }

    @Data
    public static class McpToolResult {
        private JSONObject rawResult;
        private JSONObject structuredContent;
        private List<String> textContents = new ArrayList<>();
        private boolean error;
    }
}
