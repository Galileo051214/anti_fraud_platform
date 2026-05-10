package com.anti.entity.dto;

import lombok.Data;

/**
 * 创建评论请求DTO
 */
@Data
public class CreateCommentRequest {

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 父评论ID(0表示一级评论)
     */
    private Long parentId;

    /**
     * 评论内容
     */
    private String content;
}
