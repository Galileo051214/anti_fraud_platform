package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 论坛帖子实体类
 */
@Data
@TableName(value = "forum_post", autoResultMap = true)
public class ForumPost implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

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
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> tagIds;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 是否精选:0否1是
     */
    private Integer isFeatured;

    /**
     * 是否置顶:0否1是
     */
    private Integer isTop;

    /**
     * 状态:0禁用1正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableField(exist = false)
    private String authorName;

    @TableField(exist = false)
    private String authorAvatar;

    @TableField(exist = false)
    private Boolean isLiked;
}
