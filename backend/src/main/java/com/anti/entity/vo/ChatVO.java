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
     * 消耗token数
     */
    private Integer tokensUsed;

    /**
     * 提问时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
