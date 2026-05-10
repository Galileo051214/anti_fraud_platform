package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_achievement")
public class UserAchievement implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("achievement_id")
    private Long achievementId;

    @TableField(value = "achieved_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime achievedTime;

    @TableField(exist = false)
    private Achievement achievement;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
