package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 诈骗案例实体类
 */
@Data
@TableName(value = "fraud_case", autoResultMap = true)
public class FraudCase {

    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 诈骗剧本结构(JSON决策树)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object scripts;

    /**
     * 目标年级数组,如:["大一","大二"]或["all"]
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> targetGrades;

    /**
     * 目标专业数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> targetMajors;

    /**
     * 难度等级(1-5)
     */
    private Integer difficultyLevel;

    /**
     * 风险评分(0-10)
     */
    private BigDecimal riskScore;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 点赞率
     */
    private BigDecimal likeRate;

    /**
     * 威尔逊置信度得分
     */
    private BigDecimal wilsonScore;

    /**
     * 状态:0禁用1启用
     */
    private Integer status;

    /**
     * 是否精选:0否1是
     */
    private Integer isFeatured;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
