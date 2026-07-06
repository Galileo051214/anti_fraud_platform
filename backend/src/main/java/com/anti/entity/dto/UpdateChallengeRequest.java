package com.anti.entity.dto;

import com.anti.entity.Challenge;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新闯关关卡请求DTO
 */
@Data
public class UpdateChallengeRequest {

    /**
     * 关卡名称
     */
    @Size(max = 100, message = "关卡名称不能超过100个字符")
    private String title;

    /**
     * 关卡描述
     */
    @Size(max = 500, message = "关卡描述不能超过500个字符")
    private String description;

    /**
     * 关卡顺序
     */
    @Min(value = 1, message = "关卡顺序必须大于0")
    private Integer levelOrder;

    /**
     * 难度(1-5)
     */
    @Min(value = 1, message = "难度不能小于1")
    @Max(value = 5, message = "难度不能大于5")
    private Integer difficulty;

    /**
     * 类型:quiz-答题, scenario-情景模拟, agent_scenario-Agent模拟挑战
     */
    @Size(max = 30, message = "关卡类型不能超过30个字符")
    @Pattern(regexp = "quiz|scenario|agent_scenario", message = "关卡类型只能是quiz、scenario或agent_scenario")
    private String type;

    /**
     * 及格分数
     */
    @Min(value = 0, message = "及格分数不能小于0")
    @Max(value = 100, message = "及格分数不能大于100")
    private Integer passingScore;

    /**
     * 通关奖励积分
     */
    @Min(value = 0, message = "奖励积分不能小于0")
    @Max(value = 10000, message = "奖励积分不能超过10000")
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
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
}
