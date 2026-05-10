package com.anti.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserAchievementVO implements Serializable {

    private Long id;

    private Long userId;

    private Long achievementId;

    private String achievementCode;

    private String achievementName;

    private String description;

    private String icon;

    private Integer scoreReward;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime achievedTime;

    private static final long serialVersionUID = 1L;
}
