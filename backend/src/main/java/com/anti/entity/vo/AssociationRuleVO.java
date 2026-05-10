package com.anti.entity.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 关联规则VO
 */
@Data
public class AssociationRuleVO {

    /**
     * 规则ID
     */
    private Long id;

    /**
     * 触发标签
     */
    private String triggerTag;

    /**
     * 预测标签列表
     */
    private java.util.List<String> predictedTags;

    /**
     * 置信度
     */
    private BigDecimal confidence;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 状态
     */
    private Integer status;
}
