package com.anti.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AchievementVO implements Serializable {

    private Long id;

    private String code;

    private String name;

    private String description;

    private String icon;

    private Integer scoreReward;

    private String conditionType;

    private Integer conditionValue;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private Boolean unlocked;

    private static final long serialVersionUID = 1L;
}
