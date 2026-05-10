package com.anti.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 更新帖子请求DTO
 */
@Data
public class UpdatePostRequest {

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 帖子类型
     */
    private String postType;

    /**
     * 标签ID数组
     */
    private List<Long> tagIds;

    /**
     * 状态:0禁用1正常
     */
    private Integer status;

    /**
     * 是否精选:0否1是
     */
    private Integer isFeatured;

    /**
     * 是否置顶:0否1是
     */
    private Integer isTop;
}
