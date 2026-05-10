package com.anti.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建关联规则请求DTO
 */
@Data
public class CreateAssociationRuleRequest {

    /**
     * 触发标签
     */
    private String triggerTag;

    /**
     * 预测标签数组(JSON字符串)
     */
    private String predictedTags;

    /**
     * 置信度
     */
    private BigDecimal confidence;

    /**
     * 规则描述
     */
    private String description;
}
