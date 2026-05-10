package com.anti.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 闯关进度统计VO
 */
@Data
public class ChallengeProgressVO {

    /**
     * 总关卡数
     */
    private Integer totalChallenges;

    /**
     * 已完成关卡数
     */
    private Integer completedChallenges;

    /**
     * 总得分
     */
    private Integer totalScore;

    /**
     * 下一批可挑战关卡
     */
    private List<ChallengeVO> nextChallenges;
}
