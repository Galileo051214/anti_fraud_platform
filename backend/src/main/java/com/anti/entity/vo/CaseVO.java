package com.anti.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 案例视图对象
 */
@Data
public class CaseVO {

    /**
     * 案例ID
     */
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
     * 诈骗剧本结构(JSON)
     */
    private String scripts;

    /**
     * 目标年级
     */
    private List<String> targetGrades;

    /**
     * 目标专业
     */
    private List<String> targetMajors;

    /**
     * 难度等级(1-5)
     */
    private Integer difficultyLevel;

    /**
     * 难度等级描述
     */
    private String difficultyName;

    /**
     * 风险评分
     */
    private BigDecimal riskScore;

    /**
     * 风险等级描述
     */
    private String riskLevel;

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
     * 标签列表
     */
    private List<TagVO> tags;

    /**
     * 是否精选
     */
    private Integer isFeatured;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 当前用户是否点赞
     */
    private Boolean isLiked;
}
