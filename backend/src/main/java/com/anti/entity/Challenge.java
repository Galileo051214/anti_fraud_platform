package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 闯关关卡实体类
 */
@Data
@TableName(value = "challenge", autoResultMap = true)
public class Challenge {

    /**
     * 关卡ID
     */
    @TableId(type = IdType.AUTO)
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
    @TableField(typeHandler = JacksonTypeHandler.class)
    private ChallengeContent content;

    /**
     * 情景剧本JSON(FSM状态机)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private ScenarioScript scripts;

    /**
     * 状态:0禁用1启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 题目内容结构
     */
    @Data
    public static class ChallengeContent {
        /**
         * 题目列表
         */
        private List<Question> questions;

        /**
         * 题目结构
         */
        @Data
        public static class Question {
            /**
             * 题目ID
             */
            private String id;

            /**
             * 题目类型: single-单选, multiple-多选, truefalse-判断
             */
            private String questionType;

            /**
             * 题目内容
             */
            private String text;

            /**
             * 选项列表
             */
            private List<Option> options;

            /**
             * 正确答案索引列表
             */
            private List<Integer> correctIndexes;

            /**
             * 分值
             */
            private Integer score;

            /**
             * 选项结构
             */
            @Data
            public static class Option {
                /**
                 * 选项标识
                 */
                private String label;

                /**
                 * 选项内容
                 */
                private String text;
            }
        }
    }

    /**
     * 情景剧本结构(FSM)
     */
    @Data
    public static class ScenarioScript {
        /**
         * 剧本名称
         */
        private String name;

        /**
         * 剧本描述
         */
        private String description;

        /**
         * 节点列表
         */
        private List<ScenarioNode> nodes;

        /**
         * 边列表(连接)
         */
        private List<ScenarioEdge> edges;

        /**
         * 初始节点ID
         */
        private String startNodeId;

        /**
         * 结局节点列表
         */
        private List<String> endNodeIds;

        /**
         * 节点结构
         */
        @Data
        public static class ScenarioNode {
            /**
             * 节点ID
             */
            private String id;

            /**
             * 节点类型: start-开始, dialog-对话, decision-决策, result-结果, end-结束
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
             * 角色: scammer-骗子, victim-受害者, narrator-旁白
             */
            private String role;

            /**
             * 风险提示
             */
            private String riskTip;

            /**
             * 节点在可视化画布中的位置
             */
            private Position position;

            /**
             * 节点坐标
             */
            @Data
            public static class Position {
                /**
                 * 横向坐标
                 */
                private Integer x;

                /**
                 * 纵向坐标
                 */
                private Integer y;
            }
        }

        /**
         * 边结构(连接)
         */
        @Data
        public static class ScenarioEdge {
            /**
             * 起始节点ID
             */
            private String from;

            /**
             * 目标节点ID
             */
            private String to;

            /**
             * 条件(决策选项)
             */
            private String condition;

            /**
             * 条件描述(显示给用户)
             */
            private String label;

            /**
             * 评分类型: none-剧情推进不计分, safe-安全正确选择, risk-风险错误选择
             */
            private String scoreType;

            /**
             * 是否安全选择(正确识别诈骗)
             * @deprecated 使用 scoreType 表达 none/safe/risk，保留用于兼容历史剧本
             */
            @Deprecated
            private Boolean isSafeChoice;
        }
    }
}
