package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 关联规则实体类(SPM算法用)
 * 用于存储标签之间的关联关系，预测用户可能的感兴趣内容
 */
@Data
@TableName("association_rule")
public class AssociationRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 规则ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 触发标签( antecedent )
     * 例如：用户浏览了"刷单诈骗"，则触发该标签
     */
    private String triggerTag;

    /**
     * 预测标签数组( consequent )
     * 例如：["网络博彩","跑分洗钱"]
     */
    private String predictedTags;

    /**
     * 置信度( confidence )
     * 表示当触发标签出现时，预测标签出现的概率
     * 范围：0.0000 - 1.0000
     */
    private BigDecimal confidence;

    /**
     * 规则描述
     * 对规则的文字说明
     */
    private String description;

    /**
     * 状态：0禁用 1启用
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
