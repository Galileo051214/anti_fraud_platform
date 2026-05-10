package com.anti.entity.vo;

import com.anti.entity.UserChallengeRecord;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户闯关记录视图对象
 */
@Data
public class ChallengeRecordVO {

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
     * 尝试次数
     */
    private Integer attempts;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 是否通关
     */
    private Boolean passed;

    /**
     * 答题详情
     */
    private UserChallengeRecord.AnswerDetail answerDetail;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}
