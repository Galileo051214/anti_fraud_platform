package com.anti.entity.vo;

import lombok.Data;

/**
 * 关卡统计视图对象
 */
@Data
public class ChallengeStatsVO {

    /**
     * 关卡ID
     */
    private Long challengeId;

    /**
     * 关卡名称
     */
    private String title;

    /**
     * 总参与人数
     */
    private Long totalAttempts;

    /**
     * 通过人数
     */
    private Long passedCount;

    /**
     * 通过率(百分比)
     */
    private Double passRate;

    /**
     * 平均分
     */
    private Double avgScore;

    /**
     * 最高分
     */
    private Integer maxScore;

    /**
     * 最低分
     */
    private Integer minScore;

    /**
     * 平均用时(秒)
     */
    private Double avgDuration;
}
