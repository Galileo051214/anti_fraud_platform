package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 问答会话记录实体类
 */
@Data
@TableName("qa_conversation")
public class QAConversation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话ID(用于关联同一轮对话)
     */
    private String sessionId;

    /**
     * 用户问题
     */
    private String question;

    /**
     * AI回答
     */
    private String answer;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 消耗token数
     */
    private Integer tokensUsed;

    /**
     * 用户反馈:1满意,-1不满意
     */
    private Integer feedback;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
