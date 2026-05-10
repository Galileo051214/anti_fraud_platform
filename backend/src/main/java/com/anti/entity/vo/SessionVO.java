package com.anti.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话列表项VO
 */
@Data
public class SessionVO implements Serializable {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话第一条问题摘要
     */
    private String firstQuestion;

    /**
     * AI最后一条回答摘要
     */
    private String lastAnswer;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 消耗总token
     */
    private Integer totalTokens;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
