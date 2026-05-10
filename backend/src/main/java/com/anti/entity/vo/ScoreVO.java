package com.anti.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ScoreVO implements Serializable {

    private Long id;

    private Long userId;

    private Integer totalScore;

    private Integer currentLevel;

    private Integer weeklyScore;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private Integer unlockedAchievements;

    private Integer totalAchievements;

    private static final long serialVersionUID = 1L;
}
