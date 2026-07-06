package com.anti.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * DeepSeek API封装工具类
 */
@Component
public class DeepSeekClient {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekClient.class);

    @Value("${deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.max-tokens:2048}")
    private int maxTokens;

    @Value("${deepseek.temperature:0.7}")
    private double temperature;

    @Value("${deepseek.timeout-ms:15000}")
    private int timeoutMs;

    /**
     * 发送聊天请求到DeepSeek
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @param history      历史对话记录 [[role, content], ...]
     * @return DeepSeekResponse
     */
    public DeepSeekResponse chat(String systemPrompt, String userMessage, List<String[]> history) {
        DeepSeekResponse response = new DeepSeekResponse();

        if (apiKey == null || apiKey.isBlank() || "填自己的apikey".equals(apiKey.trim())) {
            response.setSuccess(false);
            response.setErrorMessage("AI_API_KEY_MISSING");
            return response;
        }

        try {
            JSONObject requestBody = buildRequestBody(systemPrompt, userMessage, history, false);

            // 发送请求
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", "Bearer " + apiKey);

            HttpResponse httpResponse = HttpUtil.createPost(apiUrl)
                    .headerMap(headers, false)
                    .body(requestBody.toString())
                    .timeout(resolveTimeoutMs())
                    .execute();

            String result = httpResponse.body();
            if (httpResponse.getStatus() < 200 || httpResponse.getStatus() >= 300) {
                logger.warn("DeepSeek API returned non-success status: {}", httpResponse.getStatus());
                response.setSuccess(false);
                response.setErrorMessage("AI_SERVICE_UNAVAILABLE");
                return response;
            }

            logger.debug("DeepSeek API response received, length={}", result != null ? result.length() : 0);

            JSONObject responseJson = JSONUtil.parseObj(result);

            if (responseJson.containsKey("error")) {
                response.setSuccess(false);
                response.setErrorMessage("AI_SERVICE_UNAVAILABLE");
                return response;
            }

            JSONArray choices = responseJson.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                response.setContent(message.getStr("content"));
            }

            // 解析usage
            JSONObject usage = responseJson.getJSONObject("usage");
            if (usage != null) {
                response.setPromptTokens(usage.getInt("prompt_tokens", 0));
                response.setCompletionTokens(usage.getInt("completion_tokens", 0));
                response.setTotalTokens(usage.getInt("total_tokens", 0));
            }

            response.setSuccess(true);

        } catch (Exception e) {
            logger.warn("调用DeepSeek API失败: {}", e.getClass().getSimpleName());
            response.setSuccess(false);
            response.setErrorMessage("AI_SERVICE_UNAVAILABLE");
        }

        return response;
    }

    public String getModel() {
        return model;
    }

    public DeepSeekResponse chatStream(String systemPrompt,
                                       String userMessage,
                                       List<String[]> history,
                                       StreamHandler handler) {
        DeepSeekResponse response = new DeepSeekResponse();

        if (apiKey == null || apiKey.isBlank() || "填自己的apikey".equals(apiKey.trim())) {
            response.setSuccess(false);
            response.setErrorMessage("AI_API_KEY_MISSING");
            return response;
        }

        StringBuilder content = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();

        try {
            JSONObject requestBody = buildRequestBody(systemPrompt, userMessage, history, true);
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofMillis(resolveTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(resolveTimeoutMs()))
                    .build();
            java.net.http.HttpResponse<InputStream> httpResponse = client.send(
                    request,
                    java.net.http.HttpResponse.BodyHandlers.ofInputStream()
            );

            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                logger.warn("DeepSeek stream API returned non-success status: {}", httpResponse.statusCode());
                response.setSuccess(false);
                response.setErrorMessage("AI_SERVICE_UNAVAILABLE");
                return response;
            }

            try (InputStream stream = httpResponse.body();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    parseStreamLine(line, content, reasoning, response, handler);
                }
            }

            response.setContent(content.toString());
            response.setReasoningContent(reasoning.toString());
            response.setSuccess(content.length() > 0 || reasoning.length() > 0);
            if (!response.isSuccess()) {
                response.setErrorMessage("AI_SERVICE_UNAVAILABLE");
            }
        } catch (Exception e) {
            logger.warn("流式调用DeepSeek API失败: {}", e.getClass().getSimpleName());
            response.setContent(content.toString());
            response.setReasoningContent(reasoning.toString());
            response.setSuccess(content.length() > 0);
            response.setErrorMessage(response.isSuccess() ? null : "AI_SERVICE_UNAVAILABLE");
        }

        return response;
    }

    private int resolveTimeoutMs() {
        return Math.max(1000, Math.min(timeoutMs, 30000));
    }

    private JSONObject buildRequestBody(String systemPrompt, String userMessage, List<String[]> history, boolean stream) {
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", model);
        requestBody.set("max_tokens", maxTokens);
        requestBody.set("temperature", temperature);
        if (stream) {
            requestBody.set("stream", true);
            JSONObject streamOptions = new JSONObject();
            streamOptions.set("include_usage", true);
            requestBody.set("stream_options", streamOptions);
        }

        JSONArray messages = new JSONArray();
        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", systemPrompt);
        messages.add(systemMsg);

        if (history != null && !history.isEmpty()) {
            for (String[] msg : history) {
                if (msg == null || msg.length < 2 || msg[0] == null || msg[1] == null || msg[1].isBlank()) {
                    continue;
                }
                String role = msg[0].trim();
                if (!"user".equals(role) && !"assistant".equals(role)) {
                    continue;
                }
                JSONObject historyMsg = new JSONObject();
                historyMsg.set("role", role);
                historyMsg.set("content", msg[1]);
                messages.add(historyMsg);
            }
        }

        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", userMessage);
        messages.add(userMsg);

        requestBody.set("messages", messages);
        return requestBody;
    }

    private void parseStreamLine(String line,
                                 StringBuilder content,
                                 StringBuilder reasoning,
                                 DeepSeekResponse response,
                                 StreamHandler handler) {
        if (line == null) {
            return;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith(":")) {
            return;
        }
        if (!trimmed.startsWith("data:")) {
            return;
        }

        String data = trimmed.substring("data:".length()).trim();
        if (data.isEmpty() || "[DONE]".equals(data)) {
            return;
        }

        try {
            JSONObject chunk = JSONUtil.parseObj(data);
            JSONObject usage = chunk.getJSONObject("usage");
            if (usage != null) {
                response.setPromptTokens(usage.getInt("prompt_tokens", response.getPromptTokens()));
                response.setCompletionTokens(usage.getInt("completion_tokens", response.getCompletionTokens()));
                response.setTotalTokens(usage.getInt("total_tokens", response.getTotalTokens()));
            }

            JSONArray choices = chunk.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return;
            }
            JSONObject choice = choices.getJSONObject(0);
            JSONObject delta = choice == null ? null : choice.getJSONObject("delta");
            if (delta == null) {
                return;
            }

            String reasoningDelta = firstText(delta.getStr("reasoning_content"), delta.getStr("reasoning"));
            if (hasText(reasoningDelta)) {
                reasoning.append(reasoningDelta);
                if (handler != null) {
                    handler.onReasoningDelta(reasoningDelta);
                }
            }

            String contentDelta = delta.getStr("content");
            if (hasText(contentDelta)) {
                content.append(contentDelta);
                if (handler != null) {
                    handler.onContentDelta(contentDelta);
                }
            }
        } catch (Exception e) {
            logger.debug("Ignore invalid DeepSeek stream chunk");
        }
    }

    private String firstText(String first, String second) {
        if (hasText(first)) {
            return first;
        }
        return hasText(second) ? second : "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 简单对话（无历史）
     */
    public DeepSeekResponse chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, null);
    }

    public interface StreamHandler {
        default void onReasoningDelta(String delta) {
        }

        default void onContentDelta(String delta) {
        }
    }

    /**
     * DeepSeek响应结果封装
     */
    public static class DeepSeekResponse {
        private boolean success;
        private String content;
        private String reasoningContent;
        private String errorMessage;
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getReasoningContent() {
            return reasoningContent;
        }

        public void setReasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public int getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
        }

        public int getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(int completionTokens) {
            this.completionTokens = completionTokens;
        }

        public int getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}
