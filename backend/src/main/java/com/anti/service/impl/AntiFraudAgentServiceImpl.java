package com.anti.service.impl;

import com.anti.entity.vo.SourceVO;
import com.anti.service.AntiFraudAgentService;
import com.anti.util.AntiFraudPromptTemplate;
import com.anti.util.DeepSeekClient;
import com.anti.util.WebSearchClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class AntiFraudAgentServiceImpl implements AntiFraudAgentService {

    private static final List<String> LATEST_INTENT_TERMS = List.of(
            "最新", "近期", "最近", "今天", "昨日", "昨天", "本周", "本月", "今年", "当前",
            "新型", "高发", "趋势", "通报", "预警", "新闻", "汇报", "报告", "曝光"
    );
    private static final List<String> FRAUD_TERMS = List.of(
            "诈骗", "反诈", "电诈", "骗局", "骗术", "套路", "刷单", "杀猪盘", "公检法",
            "客服", "网贷", "贷款", "投资", "洗钱", "跑分", "收款码"
    );
    private static final List<String> HIGH_RISK_TERMS = List.of(
            "转账", "汇款", "打钱", "银行卡", "验证码", "共享屏幕", "屏幕共享", "远程控制",
            "下载app", "下载APP", "贷款", "保证金", "押金", "解冻金", "刷流水", "跑分",
            "公检法", "安全账户", "投资", "裸聊", "收款码"
    );
    private static final List<String> MEDIUM_RISK_TERMS = List.of(
            "客服", "兼职", "中奖", "返利", "链接", "二维码", "陌生", "账户异常", "认证",
            "征信", "退款", "校园贷"
    );

    private final DeepSeekClient deepSeekClient;
    private final WebSearchClient webSearchClient;

    public AntiFraudAgentServiceImpl(DeepSeekClient deepSeekClient, WebSearchClient webSearchClient) {
        this.deepSeekClient = deepSeekClient;
        this.webSearchClient = webSearchClient;
    }

    @Override
    public AgentAnswer answer(String question, List<String[]> historyMessages, String requestedAnswerType) {
        String answerType = resolveAnswerType(question, requestedAnswerType);
        if (ANSWER_TYPE_LATEST_REPORT.equals(answerType)) {
            return answerLatestReport(question, historyMessages);
        }
        return answerQa(question, historyMessages);
    }

    @Override
    public AgentAnswer answerStream(String question,
                                    List<String[]> historyMessages,
                                    String requestedAnswerType,
                                    AgentStreamHandler handler) {
        String answerType = resolveAnswerType(question, requestedAnswerType);
        if (ANSWER_TYPE_LATEST_REPORT.equals(answerType)) {
            return answerLatestReportStream(question, historyMessages, handler);
        }
        return answerQaStream(question, historyMessages, handler);
    }

    @Override
    public String getModelName() {
        return deepSeekClient.getModel();
    }

    private AgentAnswer answerQa(String question, List<String[]> historyMessages) {
        DeepSeekClient.DeepSeekResponse response = callDeepSeekSafely(
                AntiFraudPromptTemplate.getSystemPrompt(),
                question,
                historyMessages
        );

        AgentAnswer answer = new AgentAnswer();
        answer.setAnswerType(ANSWER_TYPE_QA);
        answer.setRiskLevel(estimateRiskLevel(question, response.getContent(), Collections.emptyList(), false));

        if (response.isSuccess() && hasText(response.getContent())) {
            answer.setAnswer(response.getContent().trim());
            answer.setTokensUsed(response.getTotalTokens());
            answer.setFallback(false);
        } else {
            answer.setAnswer(buildAiFallbackAnswer(response.getErrorMessage()));
            answer.setTokensUsed(0);
            answer.setFallback(true);
        }
        return answer;
    }

    private AgentAnswer answerQaStream(String question, List<String[]> historyMessages, AgentStreamHandler handler) {
        AgentAnswer metadata = new AgentAnswer();
        metadata.setAnswerType(ANSWER_TYPE_QA);
        emitMetadata(handler, metadata);

        DeepSeekClient.DeepSeekResponse response = callDeepSeekStreamSafely(
                AntiFraudPromptTemplate.getSystemPrompt(),
                question,
                historyMessages,
                handler
        );

        AgentAnswer answer = new AgentAnswer();
        answer.setAnswerType(ANSWER_TYPE_QA);
        answer.setReasoning(trimToNull(response.getReasoningContent()));
        answer.setRiskLevel(estimateRiskLevel(question, response.getContent(), Collections.emptyList(), false));

        if (response.isSuccess() && hasText(response.getContent())) {
            answer.setAnswer(response.getContent().trim());
            answer.setTokensUsed(response.getTotalTokens());
            answer.setFallback(false);
        } else {
            String fallbackAnswer = buildAiFallbackAnswer(response.getErrorMessage());
            emitContent(handler, fallbackAnswer);
            answer.setAnswer(fallbackAnswer);
            answer.setTokensUsed(0);
            answer.setFallback(true);
        }
        return answer;
    }

    private AgentAnswer answerLatestReport(String question, List<String[]> historyMessages) {
        WebSearchClient.SearchResult searchResult = searchSafely(question);
        List<SourceVO> sources = safeSources(searchResult.getSources());
        String userPrompt = AntiFraudPromptTemplate.buildLatestReportPrompt(
                question,
                sources,
                Boolean.TRUE.equals(searchResult.getFallback())
        );
        DeepSeekClient.DeepSeekResponse response = callDeepSeekSafely(
                AntiFraudPromptTemplate.getLatestReportSystemPrompt(),
                userPrompt,
                historyMessages
        );

        AgentAnswer answer = new AgentAnswer();
        answer.setAnswerType(ANSWER_TYPE_LATEST_REPORT);
        answer.setSources(sources);
        answer.setRetrievedAt(searchResult.getRetrievedAt());
        answer.setRiskLevel(estimateRiskLevel(question, response.getContent(), sources, true));
        answer.setFallback(Boolean.TRUE.equals(searchResult.getFallback()));
        answer.setFallbackReason(searchResult.getFallbackReason());
        answer.setSearchProvider(searchResult.getProvider());

        if (response.isSuccess() && hasText(response.getContent())) {
            answer.setAnswer(response.getContent().trim());
            answer.setTokensUsed(response.getTotalTokens());
        } else {
            answer.setAnswer(buildLatestReportFallbackAnswer(sources, searchResult));
            answer.setTokensUsed(0);
            answer.setFallback(true);
        }
        return answer;
    }

    private AgentAnswer answerLatestReportStream(String question, List<String[]> historyMessages, AgentStreamHandler handler) {
        emitReasoning(handler, "已判断该问题需要检索最新公开来源，开始进行 WebSearch。\n");
        WebSearchClient.SearchResult searchResult = searchSafely(question);
        List<SourceVO> sources = safeSources(searchResult.getSources());
        emitReasoning(handler, buildSearchProcessSummary(sources, searchResult));

        AgentAnswer metadata = new AgentAnswer();
        metadata.setAnswerType(ANSWER_TYPE_LATEST_REPORT);
        metadata.setSources(sources);
        metadata.setRetrievedAt(searchResult.getRetrievedAt());
        metadata.setRiskLevel("medium");
        metadata.setFallback(Boolean.TRUE.equals(searchResult.getFallback()));
        metadata.setFallbackReason(searchResult.getFallbackReason());
        metadata.setSearchProvider(searchResult.getProvider());
        emitMetadata(handler, metadata);

        String userPrompt = AntiFraudPromptTemplate.buildLatestReportPrompt(
                question,
                sources,
                Boolean.TRUE.equals(searchResult.getFallback())
        );
        DeepSeekClient.DeepSeekResponse response = callDeepSeekStreamSafely(
                AntiFraudPromptTemplate.getLatestReportSystemPrompt(),
                userPrompt,
                historyMessages,
                handler
        );

        AgentAnswer answer = new AgentAnswer();
        answer.setAnswerType(ANSWER_TYPE_LATEST_REPORT);
        answer.setSources(sources);
        answer.setRetrievedAt(searchResult.getRetrievedAt());
        answer.setReasoning(trimToNull(response.getReasoningContent()));
        answer.setRiskLevel(estimateRiskLevel(question, response.getContent(), sources, true));
        answer.setFallback(Boolean.TRUE.equals(searchResult.getFallback()));
        answer.setFallbackReason(searchResult.getFallbackReason());
        answer.setSearchProvider(searchResult.getProvider());

        if (response.isSuccess() && hasText(response.getContent())) {
            answer.setAnswer(response.getContent().trim());
            answer.setTokensUsed(response.getTotalTokens());
        } else {
            String fallbackAnswer = buildLatestReportFallbackAnswer(sources, searchResult);
            emitContent(handler, fallbackAnswer);
            answer.setAnswer(fallbackAnswer);
            answer.setTokensUsed(0);
            answer.setFallback(true);
        }
        return answer;
    }

    private WebSearchClient.SearchResult searchSafely(String question) {
        try {
            return webSearchClient.searchLatestFraud(question);
        } catch (Exception e) {
            log.warn("最新诈骗汇报检索失败，已降级: {}", e.getClass().getSimpleName());
            WebSearchClient.SearchResult result = new WebSearchClient.SearchResult();
            result.setFallback(true);
            result.setFallbackReason("WEBSEARCH_UNAVAILABLE");
            result.setRetrievedAt(LocalDateTime.now());
            result.setSources(Collections.emptyList());
            return result;
        }
    }

    private DeepSeekClient.DeepSeekResponse callDeepSeekSafely(String systemPrompt,
                                                               String userMessage,
                                                               List<String[]> historyMessages) {
        try {
            return deepSeekClient.chat(systemPrompt, userMessage, historyMessages);
        } catch (Exception e) {
            log.warn("调用AI服务失败，已降级为本地提示: {}", e.getClass().getSimpleName());
            DeepSeekClient.DeepSeekResponse response = new DeepSeekClient.DeepSeekResponse();
            response.setSuccess(false);
            response.setErrorMessage("AI_SERVICE_UNAVAILABLE");
            return response;
        }
    }

    private DeepSeekClient.DeepSeekResponse callDeepSeekStreamSafely(String systemPrompt,
                                                                     String userMessage,
                                                                     List<String[]> historyMessages,
                                                                     AgentStreamHandler handler) {
        try {
            return deepSeekClient.chatStream(
                    systemPrompt,
                    userMessage,
                    historyMessages,
                    new DeepSeekClient.StreamHandler() {
                        @Override
                        public void onReasoningDelta(String delta) {
                            if (handler != null) {
                                handler.onReasoningDelta(delta);
                            }
                        }

                        @Override
                        public void onContentDelta(String delta) {
                            if (handler != null) {
                                handler.onContentDelta(delta);
                            }
                        }
                    }
            );
        } catch (Exception e) {
            log.warn("流式调用AI服务失败，已降级为本地提示: {}", e.getClass().getSimpleName());
            DeepSeekClient.DeepSeekResponse response = new DeepSeekClient.DeepSeekResponse();
            response.setSuccess(false);
            response.setErrorMessage("AI_SERVICE_UNAVAILABLE");
            return response;
        }
    }

    private String resolveAnswerType(String question, String requestedAnswerType) {
        if (ANSWER_TYPE_QA.equals(requestedAnswerType)) {
            return ANSWER_TYPE_QA;
        }
        if (ANSWER_TYPE_LATEST_REPORT.equals(requestedAnswerType)) {
            return ANSWER_TYPE_LATEST_REPORT;
        }
        return wantsLatestReport(question) ? ANSWER_TYPE_LATEST_REPORT : ANSWER_TYPE_QA;
    }

    private boolean wantsLatestReport(String question) {
        if (!hasText(question)) {
            return false;
        }
        String normalized = question.toLowerCase(Locale.ROOT);
        return containsAny(normalized, LATEST_INTENT_TERMS) && containsAny(normalized, FRAUD_TERMS);
    }

    private String estimateRiskLevel(String question, String answer, List<SourceVO> sources, boolean latestReport) {
        String text = ((question == null ? "" : question) + "\n" + (answer == null ? "" : answer)).toLowerCase(Locale.ROOT);
        if (containsAny(text, HIGH_RISK_TERMS)) {
            return "high";
        }
        if (containsAny(text, MEDIUM_RISK_TERMS) || latestReport || !safeSources(sources).isEmpty()) {
            return "medium";
        }
        return "low";
    }

    private boolean containsAny(String text, List<String> terms) {
        if (!hasText(text)) {
            return false;
        }
        for (String term : terms) {
            if (text.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private List<SourceVO> safeSources(List<SourceVO> sources) {
        return sources == null ? new ArrayList<>() : new ArrayList<>(sources);
    }

    private String buildLatestReportFallbackAnswer(List<SourceVO> sources, WebSearchClient.SearchResult searchResult) {
        if (sources == null || sources.isEmpty()) {
            return "暂时无法获取最新官方诈骗通报。请优先通过公安机关、12321举报中心等官方渠道核验；如已经转账、泄露验证码或共享屏幕，请立即停止操作并拨打110。";
        }

        StringBuilder answer = new StringBuilder();
        if (Boolean.TRUE.equals(searchResult.getFallback())) {
            answer.append("实时检索暂时不可用，以下为可核验的官方来源，不代表最新完整通报。\n\n");
        } else {
            answer.append("已获取到官方来源，但AI总结暂时不可用。你可以先按以下来源核验：\n\n");
        }
        for (int i = 0; i < sources.size(); i++) {
            SourceVO source = sources.get(i);
            answer.append(i + 1).append(". ")
                    .append(defaultText(source.getTitle(), source.getDomain()))
                    .append("：")
                    .append(defaultText(source.getUrl(), ""))
                    .append("\n");
        }
        answer.append("\n涉及转账、验证码、屏幕共享、贷款解冻金、保证金等要求时，先按高风险处理并通过官方渠道二次核验。");
        return answer.toString();
    }

    private String buildAiFallbackAnswer(String errorMessage) {
        if ("AI_API_KEY_MISSING".equals(errorMessage)) {
            return "AI服务尚未配置，请联系管理员配置后再使用。";
        }
        return "AI服务暂时不可用，请稍后再试。你也可以先查看资讯、案例和闯关内容获取反诈建议。";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private void emitMetadata(AgentStreamHandler handler, AgentAnswer metadata) {
        if (handler != null) {
            handler.onMetadata(metadata);
        }
    }

    private void emitContent(AgentStreamHandler handler, String content) {
        if (handler != null && hasText(content)) {
            handler.onContentDelta(content);
        }
    }

    private void emitReasoning(AgentStreamHandler handler, String reasoning) {
        if (handler != null && hasText(reasoning)) {
            handler.onReasoningDelta(reasoning);
        }
    }

    private String buildSearchProcessSummary(List<SourceVO> sources, WebSearchClient.SearchResult searchResult) {
        StringBuilder summary = new StringBuilder();
        summary.append("检索结果：");
        if (searchResult != null && hasText(searchResult.getProvider())) {
            summary.append("使用 ").append(searchResult.getProvider()).append("。");
        }
        if (searchResult != null && Boolean.TRUE.equals(searchResult.getFallback())) {
            summary.append("检索已降级");
            if (hasText(searchResult.getFallbackReason())) {
                summary.append("（").append(searchResult.getFallbackReason()).append("）");
            }
            summary.append("。");
        }
        summary.append("\n");

        List<SourceVO> safeSources = safeSources(sources);
        if (safeSources.isEmpty()) {
            summary.append("未获得可用页面，后续回答会明确说明检索结果有限。\n");
            return summary.toString();
        }

        int limit = Math.min(safeSources.size(), 5);
        for (int i = 0; i < limit; i++) {
            SourceVO source = safeSources.get(i);
            summary.append("访问页面 ")
                    .append(i + 1)
                    .append("：")
                    .append(defaultText(source.getTitle(), source.getDomain()));
            if (hasText(source.getUrl())) {
                summary.append("（").append(source.getUrl()).append("）");
            }
            String description = summarizeSource(source);
            if (hasText(description)) {
                summary.append("。页面要点：").append(description);
            }
            summary.append("\n");
        }
        return summary.toString();
    }

    private String summarizeSource(SourceVO source) {
        if (source == null) {
            return "";
        }
        String text = firstText(source.getSnippet(), source.getContent());
        if (!hasText(text)) {
            return "";
        }
        return abbreviate(text.replaceAll("\\s+", " ").trim(), 120);
    }

    private String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }
}
