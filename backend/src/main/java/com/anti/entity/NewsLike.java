package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资讯点赞实体类
 */
@Data
@TableName("news_like")
public class NewsLike implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("news_id")
    private Long newsId;

    @TableField("user_id")
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
