package com.anti.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建帖子请求DTO
 */
@Data
public class CreatePostRequest {

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 帖子类型: experience-经验分享, question-提问, discussion-讨论
     */
    private String postType;

    /**
     * 标签ID数组
     */
    private List<Long> tagIds;
}
