package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_profile")
public class UserProfile implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String grade;

    private String major;

    @TableField("knowledge_level")
    private Integer knowledgeLevel;

    @TableField("weak_points")
    private String weakPoints;

    @TableField("interest_tags")
    private String interestTags;

    @TableField("lifecycle_stage")
    private String lifecycleStage;

    @TableField("browse_count")
    private Integer browseCount;

    @TableField("register_days")
    private Integer registerDays;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
