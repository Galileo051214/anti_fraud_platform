package com.anti.entity.dto;

import lombok.Data;

/**
 * 情景模拟决策请求DTO
 */
@Data
public class ScenarioDecisionRequest {

    /**
     * 关卡ID
     */
    private Long challengeId;

    /**
     * 当前节点ID
     */
    private String currentNode;

    /**
     * 选择的边ID
     */
    private String selectedEdgeId;

    /**
     * 开始时间戳
     */
    private Long startTime;
}
