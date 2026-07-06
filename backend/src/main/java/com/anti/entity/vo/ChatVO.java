package com.anti.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天响应VO
 */
@Data
public class ChatVO implements Serializable {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 问题
     */
    private String question;

    /**
     * AI回答
     */
    private String answer;

    /**
     * 模型返回的可展示分析内容
     */
    private String reasoning;

    /**
     * 消耗token数
     */
    private Integer tokensUsed;

    /**
     * 是否为AI服务不可用时的本地降级回答
     */
    private Boolean fallback;

    /**
     * 降级原因
     */
    private String fallbackReason;

    /**
     * 回答类型：qa / latest_report
     */
    private String answerType;

    /**
     * 实际使用的检索 provider
     */
    private String searchProvider;

    /**
     * 风险等级：low / medium / high
     */
    private String riskLevel;

    /**
     * 检索来源
     */
    private List<SourceVO> sources;

    /**
     * 检索时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime retrievedAt;

    /**
     * 提问时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
