package com.anti.service;

import com.anti.entity.vo.SourceVO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 简单反诈 agent：根据问题选择普通问答或最新诈骗汇报。
 */
public interface AntiFraudAgentService {

    String ANSWER_TYPE_AUTO = "auto";
    String ANSWER_TYPE_QA = "qa";
    String ANSWER_TYPE_LATEST_REPORT = "latest_report";

    AgentAnswer answer(String question, List<String[]> historyMessages, String requestedAnswerType);

    AgentAnswer answerStream(String question,
                             List<String[]> historyMessages,
                             String requestedAnswerType,
                             AgentStreamHandler handler);

    String getModelName();

    interface AgentStreamHandler {
        default void onMetadata(AgentAnswer metadata) {
        }

        default void onReasoningDelta(String delta) {
        }

        default void onContentDelta(String delta) {
        }
    }

    @Data
    class AgentAnswer implements Serializable {
        private String answer;
        private String reasoning;
        private Integer tokensUsed = 0;
        private Boolean fallback = false;
        private String fallbackReason;
        private String answerType = ANSWER_TYPE_QA;
        private String searchProvider;
        private String riskLevel = "low";
        private LocalDateTime retrievedAt;
        private List<SourceVO> sources = new ArrayList<>();

        private static final long serialVersionUID = 1L;
    }
}
