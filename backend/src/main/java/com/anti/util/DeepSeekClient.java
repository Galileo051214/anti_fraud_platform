package com.anti.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", model);
            requestBody.set("max_tokens", maxTokens);
            requestBody.set("temperature", temperature);

            JSONArray messages = new JSONArray();

            // 添加系统提示词
            JSONObject systemMsg = new JSONObject();
            systemMsg.set("role", "system");
            systemMsg.set("content", systemPrompt);
            messages.add(systemMsg);

            // 添加历史对话
            if (history != null && !history.isEmpty()) {
                for (String[] msg : history) {
                    JSONObject historyMsg = new JSONObject();
                    historyMsg.set("role", msg[0]);
                    historyMsg.set("content", msg[1]);
                    messages.add(historyMsg);
                }
            }

            // 添加当前用户消息
            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", userMessage);
            messages.add(userMsg);

            requestBody.set("messages", messages);

            // 发送请求
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", "Bearer " + apiKey);

            String result = HttpUtil.createPost(apiUrl)
                    .headerMap(headers, false)
                    .body(requestBody.toString())
                    .timeout(60000)
                    .execute()
                    .body();

            logger.debug("DeepSeek API response: {}", result);

            JSONObject responseJson = JSONUtil.parseObj(result);

            if (responseJson.containsKey("error")) {
                response.setSuccess(false);
                response.setErrorMessage(responseJson.getStr("error", "未知错误"));
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
            logger.error("调用DeepSeek API失败", e);
            response.setSuccess(false);
            response.setErrorMessage("API调用失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 简单对话（无历史）
     */
    public DeepSeekResponse chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, null);
    }

    /**
     * DeepSeek响应结果封装
     */
    public static class DeepSeekResponse {
        private boolean success;
        private String content;
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
