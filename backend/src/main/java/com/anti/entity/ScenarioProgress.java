package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 情景模拟进度表实体类
 */
@Data
@TableName(value = "scenario_progress", autoResultMap = true)
public class ScenarioProgress {

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 情景模拟ID(关卡ID)
     */
    private Long challengeId;

    /**
     * 当前节点ID
     */
    private String currentNode;

    /**
     * 决策历史JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<DecisionRecord> decisionHistory;

    /**
     * 状态: in_progress-进行中, completed-已完成(好结局), failed-失败(坏结局)
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 最终得分(基于决策计算)
     */
    private Integer finalScore;

    /**
     * 决策记录结构
     */
    @Data
    public static class DecisionRecord {
        /**
         * 节点ID
         */
        private String nodeId;

        /**
         * 选择的下一步边ID
         */
        private String edgeId;

        /**
         * 选择描述
         */
        private String choiceLabel;

        /**
         * 是否为安全选择
         */
        private Boolean isSafeChoice;

        /**
         * 决策时间
         */
        private String timestamp;
    }
}
