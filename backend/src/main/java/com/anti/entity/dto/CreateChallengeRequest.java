package com.anti.entity.dto;

import com.anti.entity.Challenge;
import lombok.Data;

import java.util.List;

/**
 * 创建闯关关卡请求DTO
 */
@Data
public class CreateChallengeRequest {

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
     * 类型:quiz-答题, scenario-情景模拟
     */
    private String type;

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
}
