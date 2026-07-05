package com.anti.entity.vo;

import com.anti.entity.Challenge;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 闯关关卡视图对象
 */
@Data
public class ChallengeVO {

    /**
     * 关卡ID
     */
    private Long id;

    /**
     * 关卡名称
     */
    private String title;

    /**
     * 关卡描述
     */
    private String description;

    /**
     * 关卡顺序
     */
    private Integer levelOrder;

    /**
     * 难度(1-5)
     */
    private Integer difficulty;

    /**
     * 难度名称
     */
    private String difficultyName;

    /**
     * 类型:quiz-答题, scenario-情景模拟, agent_scenario-Agent模拟挑战
     */
    private String type;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 及格分数
     */
    private Integer passingScore;

    /**
     * 通关奖励积分
     */
    private Integer scoreReward;

    /**
     * 题目JSON
     */
    private Challenge.ChallengeContent content;

    /**
     * 情景剧本JSON
     */
    private Challenge.ScenarioScript scripts;

    /**
     * Agent情景模拟配置JSON
     */
    private Challenge.AgentConfig agentConfig;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 用户是否已通关
     */
    private Boolean passed;

    /**
     * 用户历史最高分
     */
    private Integer highestScore;

    /**
     * 是否锁定
     */
    private Boolean locked;

    /**
     * 解锁条件描述
     */
    private String unlockHint;

    /**
     * 获取难度名称
     */
    public static String getDifficultyName(int level) {
        return switch (level) {
            case 1 -> "入门";
            case 2 -> "简单";
            case 3 -> "中等";
            case 4 -> "困难";
            case 5 -> "噩梦";
            default -> "未知";
        };
    }

    /**
     * 获取类型名称
     */
    public static String getTypeName(String type) {
        return switch (type) {
            case "quiz" -> "答题挑战";
            case "scenario" -> "情景模拟";
            case "agent_scenario" -> "Agent模拟";
            default -> "未知";
        };
    }
}
