package com.anti.service;

import com.anti.entity.dto.ScenarioDecisionRequest;
import com.anti.entity.vo.ScenarioProgressVO;

/**
 * 情景模拟服务接口(FSM状态机)
 */
public interface ScenarioService {

    /**
     * 开始情景模拟
     */
    ScenarioProgressVO startScenario(Long challengeId, Long userId);

    /**
     * 获取当前进度
     */
    ScenarioProgressVO getProgress(Long challengeId, Long userId);

    /**
     * 做出决策
     */
    ScenarioProgressVO makeDecision(ScenarioDecisionRequest request, Long userId);

    /**
     * 重置情景模拟
     */
    void resetScenario(Long challengeId, Long userId);

    /**
     * 获取情景模拟结局
     */
    ScenarioProgressVO getEnding(Long challengeId, Long userId);
}
