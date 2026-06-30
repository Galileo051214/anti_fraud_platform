package com.anti.entity.vo;

import com.anti.entity.Challenge;
import com.anti.entity.ScenarioProgress;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 情景模拟进度视图对象
 */
@Data
public class ScenarioProgressVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 关卡ID
     */
    private Long challengeId;

    /**
     * 关卡名称
     */
    private String challengeTitle;

    /**
     * 当前节点ID
     */
    private String currentNode;

    /**
     * 当前节点详情
     */
    private ScenarioNodeVO currentNodeDetail;

    /**
     * 可选的下一步
     */
    private List<ScenarioEdgeVO> availableChoices;

    /**
     * 脱敏后的完整剧本流程图，仅用于前端渲染节点连线，不包含正确答案标记
     */
    private Challenge.ScenarioScript script;

    /**
     * 决策历史
     */
    private List<ScenarioProgress.DecisionRecord> decisionHistory;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusName;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 最终得分
     */
    private Integer finalScore;

    /**
     * 是否通关
     */
    private Boolean passed;

    /**
     * 获得积分
     */
    private Integer earnedScore;

    /**
     * 关卡难度(1-5)
     */
    private Integer difficulty;

    /**
     * 难度名称
     */
    private String difficultyName;

    /**
     * 及格分数
     */
    private Integer passingScore;

    /**
     * 积分奖励
     */
    private Integer scoreReward;

    /**
     * 节点视图对象
     */
    @Data
    public static class ScenarioNodeVO {
        /**
         * 节点ID
         */
        private String id;

        /**
         * 节点类型
         */
        private String type;

        /**
         * 节点标题
         */
        private String title;

        /**
         * 节点内容
         */
        private String content;

        /**
         * 角色
         */
        private String role;

        /**
         * 风险提示
         */
        private String riskTip;

        /**
         * 节点坐标
         */
        private Challenge.ScenarioScript.ScenarioNode.Position position;
    }

    /**
     * 边视图对象
     */
    @Data
    public static class ScenarioEdgeVO {
        /**
         * 边ID(from_to)
         */
        private String edgeId;

        /**
         * 目标节点ID
         */
        private String toNode;

        /**
         * 选择描述
         */
        private String label;

        /**
         * 条件描述
         */
        private String condition;
    }

    /**
     * 获取状态名称
     */
    public static String getStatusName(String status) {
        return switch (status) {
            case "in_progress" -> "进行中";
            case "completed" -> "成功脱身";
            case "failed" -> "不幸中招";
            default -> "未知";
        };
    }
}
