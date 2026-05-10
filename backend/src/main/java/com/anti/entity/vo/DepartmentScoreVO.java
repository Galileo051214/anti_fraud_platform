package com.anti.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 院系得分对比VO
 */
@Data
public class DepartmentScoreVO {

    /**
     * 院系列表
     */
    private List<String> departments;

    /**
     * 平均得分列表
     */
    private List<BigDecimal> avgScores;

    /**
     * 用户数列表
     */
    private List<Integer> userCounts;

    /**
     * 完成率列表
     */
    private List<BigDecimal> completionRates;
}
