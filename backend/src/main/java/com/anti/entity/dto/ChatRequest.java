package com.anti.entity.dto;

import lombok.Data;

/**
 * 聊天请求DTO
 */
@Data
public class ChatRequest {

    /**
     * 问题内容
     */
    private String question;

    /**
     * 会话ID(可选，传入则继续该会话)
     */
    private String sessionId;
}
