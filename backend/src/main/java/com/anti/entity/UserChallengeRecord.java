package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户闯关记录实体类
 */
@Data
@TableName(value = "user_challenge_record", autoResultMap = true)
public class UserChallengeRecord {

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
     * 关卡ID
     */
    private Long challengeId;

    /**
     * 尝试次数
     */
    private Integer attempts;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 是否通关:0否1是
     */
    private Integer passed;

    /**
     * 答题详情JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private AnswerDetail answerDetail;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 答题详情结构
     */
    @Data
    public static class AnswerDetail {
        /**
         * 题目答案列表
         */
        private List<QuestionAnswer> answers;

        /**
         * 总分
         */
        private Integer totalScore;

        /**
         * 满分
         */
        private Integer maxScore;

        /**
         * 正确题数
         */
        private Integer correctCount;

        /**
         * 题目答案结构
         */
        @Data
        public static class QuestionAnswer {
            /**
             * 题目ID
             */
            private String questionId;

            /**
             * 用户选择索引列表
             */
            private List<Integer> selectedIndexes;

            /**
             * 正确答案索引列表
             */
            private List<Integer> correctIndexes;

            /**
             * 是否正确
             */
            private Boolean correct;

            /**
             * 得分
             */
            private Integer score;
        }
    }
}
