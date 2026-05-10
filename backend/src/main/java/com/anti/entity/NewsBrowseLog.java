package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("news_browse_log")
public class NewsBrowseLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("news_id")
    private Long newsId;

    @TableField("browse_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime browseTime;

    @TableField("stay_duration")
    private Integer stayDuration;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
