package com.anti.entity.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Token统计VO
 */
@Data
public class TokenStatsVO implements Serializable {

    /**
     * 总消耗token
     */
    private Integer totalTokens;

    /**
     * 总提问次数
     */
    private Integer totalQuestions;

    /**
     * 满意次数
     */
    private Integer satisfiedCount;

    /**
     * 不满意次数
     */
    private Integer dissatisfiedCount;

    private static final long serialVersionUID = 1L;
}
