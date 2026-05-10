package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资讯实体类
 */
@Data
@TableName("news")
public class News implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String summary;

    @TableField("cover_image")
    private String coverImage;

    @TableField("category_id")
    private Long categoryId;

    @TableField("author_id")
    private Long authorId;

    @TableField("news_type")
    private String newsType;

    @TableField("is_top")
    private Integer isTop;

    @TableField("is_mandatory")
    private Integer isMandatory;

    @TableField("view_count")
    private Integer viewCount;

    private Integer status;

    @TableField("publish_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String authorName;

    @TableField(exist = false)
    private Long likeCount;

    @TableField(exist = false)
    private Boolean isLiked;
}
