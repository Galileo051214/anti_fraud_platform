package com.anti.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 更新案例请求DTO
 */
@Data
public class UpdateCaseRequest {

    /**
     * 案例标题
     */
    private String title;

    /**
     * 案例类型
     */
    private String caseType;

    /**
     * 案例详情内容
     */
    private String content;

    /**
     * 诈骗剧本结构(JSON)
     */
    private String scripts;

    /**
     * 目标年级数组
     */
    private List<String> targetGrades;

    /**
     * 目标专业数组
     */
    private List<String> targetMajors;

    /**
     * 难度等级(1-5)
     */
    private Integer difficultyLevel;

    /**
     * 风险评分(0-10)
     */
    private java.math.BigDecimal riskScore;

    /**
     * 标签ID数组
     */
    private List<Long> tagIds;

    /**
     * 状态:0禁用1启用
     */
    private Integer status;

    /**
     * 是否精选:0否1是
     */
    private Integer isFeatured;
}
