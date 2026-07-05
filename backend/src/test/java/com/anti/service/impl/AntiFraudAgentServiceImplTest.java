package com.anti.service.impl;

import com.anti.entity.vo.SourceVO;
import com.anti.service.AntiFraudAgentService;
import com.anti.util.DeepSeekClient;
import com.anti.util.WebSearchClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AntiFraudAgentServiceImplTest {

    @Mock
    private DeepSeekClient deepSeekClient;
    @Mock
    private WebSearchClient webSearchClient;

    @Test
    void autoLatestReportTriggersWebSearchAndReturnsSources() {
        AntiFraudAgentServiceImpl service = new AntiFraudAgentServiceImpl(deepSeekClient, webSearchClient);
        LocalDateTime retrievedAt = LocalDateTime.of(2026, 7, 4, 9, 0);
        SourceVO source = source("公安部最新反诈提醒", "https://www.mps.gov.cn/report.html", "mps.gov.cn", "近期诈骗提醒");
        WebSearchClient.SearchResult searchResult = new WebSearchClient.SearchResult();
        searchResult.setSources(List.of(source));
        searchResult.setRetrievedAt(retrievedAt);
        searchResult.setFallback(false);
        when(webSearchClient.searchLatestFraud("最新诈骗汇报有哪些")).thenReturn(searchResult);
        when(deepSeekClient.chat(anyString(), anyString(), any())).thenReturn(successResponse("最新汇报正文", 31));

        AntiFraudAgentService.AgentAnswer answer = service.answer("最新诈骗汇报有哪些", List.of(), "auto");

        assertThat(answer.getAnswerType()).isEqualTo(AntiFraudAgentService.ANSWER_TYPE_LATEST_REPORT);
        assertThat(answer.getAnswer()).isEqualTo("最新汇报正文");
        assertThat(answer.getSources()).containsExactly(source);
        assertThat(answer.getRetrievedAt()).isEqualTo(retrievedAt);
        assertThat(answer.getFallback()).isFalse();
        assertThat(answer.getRiskLevel()).isEqualTo("medium");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(deepSeekClient).chat(anyString(), promptCaptor.capture(), any());
        assertThat(promptCaptor.getValue()).contains("https://www.mps.gov.cn/report.html", "公安部最新反诈提醒");
    }

    @Test
    void ordinaryQuestionDoesNotUseWebSearch() {
        AntiFraudAgentServiceImpl service = new AntiFraudAgentServiceImpl(deepSeekClient, webSearchClient);
        when(deepSeekClient.chat(anyString(), eq("如何识别刷单诈骗"), any())).thenReturn(successResponse("不要垫资刷单", 12));

        AntiFraudAgentService.AgentAnswer answer = service.answer("如何识别刷单诈骗", List.of(), "auto");

        assertThat(answer.getAnswerType()).isEqualTo(AntiFraudAgentService.ANSWER_TYPE_QA);
        assertThat(answer.getAnswer()).isEqualTo("不要垫资刷单");
        assertThat(answer.getSources()).isEmpty();
        assertThat(answer.getFallback()).isFalse();
        verifyNoInteractions(webSearchClient);
    }

    @Test
    void latestReportUsesLocalAnswerWhenAiFails() {
        AntiFraudAgentServiceImpl service = new AntiFraudAgentServiceImpl(deepSeekClient, webSearchClient);
        SourceVO source = source("12321举报中心", "https://www.12321.cn/", "12321.cn", "举报诈骗短信");
        WebSearchClient.SearchResult searchResult = new WebSearchClient.SearchResult();
        searchResult.setSources(List.of(source));
        searchResult.setRetrievedAt(LocalDateTime.of(2026, 7, 4, 11, 0));
        searchResult.setFallback(true);
        searchResult.setFallbackReason("WEBSEARCH_API_KEY_MISSING");
        when(webSearchClient.searchLatestFraud("最新反诈预警")).thenReturn(searchResult);
        when(deepSeekClient.chat(anyString(), anyString(), any())).thenReturn(failedResponse("AI_API_KEY_MISSING"));

        AntiFraudAgentService.AgentAnswer answer = service.answer(
                "最新反诈预警",
                List.of(),
                AntiFraudAgentService.ANSWER_TYPE_LATEST_REPORT
        );

        assertThat(answer.getFallback()).isTrue();
        assertThat(answer.getAnswer()).contains("实时检索暂时不可用", "12321举报中心");
        assertThat(answer.getSources()).containsExactly(source);
        assertThat(answer.getTokensUsed()).isZero();
    }

    @Test
    void streamLatestReportEmitsSearchProcessReasoning() {
        AntiFraudAgentServiceImpl service = new AntiFraudAgentServiceImpl(deepSeekClient, webSearchClient);
        SourceVO source = source(
                "公安部最新反诈提醒",
                "https://www.mps.gov.cn/report.html",
                "mps.gov.cn",
                "页面介绍近期高发诈骗类型和防范提醒"
        );
        WebSearchClient.SearchResult searchResult = new WebSearchClient.SearchResult();
        searchResult.setSources(List.of(source));
        searchResult.setRetrievedAt(LocalDateTime.of(2026, 7, 5, 10, 0));
        searchResult.setProvider("firecrawl-mcp");
        when(webSearchClient.searchLatestFraud("近期高发诈骗有哪些")).thenReturn(searchResult);
        when(deepSeekClient.chatStream(anyString(), anyString(), any(), any()))
                .thenAnswer(invocation -> successResponse("流式正文", 16));

        StringBuilder reasoning = new StringBuilder();
        AntiFraudAgentService.AgentAnswer answer = service.answerStream(
                "近期高发诈骗有哪些",
                List.of(),
                "auto",
                new AntiFraudAgentService.AgentStreamHandler() {
                    @Override
                    public void onReasoningDelta(String delta) {
                        reasoning.append(delta);
                    }
                }
        );

        assertThat(answer.getAnswerType()).isEqualTo(AntiFraudAgentService.ANSWER_TYPE_LATEST_REPORT);
        assertThat(reasoning.toString())
                .contains("开始进行 WebSearch")
                .contains("访问页面 1")
                .contains("https://www.mps.gov.cn/report.html")
                .contains("页面要点");
    }

    private DeepSeekClient.DeepSeekResponse successResponse(String content, int totalTokens) {
        DeepSeekClient.DeepSeekResponse response = new DeepSeekClient.DeepSeekResponse();
        response.setSuccess(true);
        response.setContent(content);
        response.setTotalTokens(totalTokens);
        return response;
    }

    private DeepSeekClient.DeepSeekResponse failedResponse(String errorMessage) {
        DeepSeekClient.DeepSeekResponse response = new DeepSeekClient.DeepSeekResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }

    private SourceVO source(String title, String url, String domain, String snippet) {
        SourceVO source = new SourceVO();
        source.setTitle(title);
        source.setUrl(url);
        source.setDomain(domain);
        source.setSnippet(snippet);
        return source;
    }
}
