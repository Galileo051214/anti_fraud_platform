package com.anti.entity.dto;

import lombok.Data;

/**
 * 用户反馈请求DTO
 */
@Data
public class FeedbackRequest {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 反馈:1满意,-1不满意
     */
    private Integer feedback;
}
