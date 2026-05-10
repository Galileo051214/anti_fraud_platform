package com.anti.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 关卡概览统计视图对象
 */
@Data
public class ChallengeOverviewVO {

    /**
     * 总关卡数
     */
    private Long totalChallenges;

    /**
     * 启用关卡数
     */
    private Long enabledChallenges;

    /**
     * 禁用关卡数
     */
    private Long disabledChallenges;

    /**
     * 答题挑战数
     */
    private Long quizChallenges;

    /**
     * 情景模拟数
     */
    private Long scenarioChallenges;

    /**
     * 总参与次数
     */
    private Long totalAttempts;

    /**
     * 总通关人数
     */
    private Long totalPassedUsers;

    /**
     * 整体通过率
     */
    private Double overallPassRate;

    /**
     * 今日通关数
     */
    private Long todayPassed;

    /**
     * 各关卡统计列表
     */
    private List<ChallengeStatsVO> challengeStats;
}
