package com.anti.entity.vo;

import lombok.Data;

/**
 * TOP案例VO
 */
@Data
public class TopCaseVO {

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
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 热度得分
     */
    private Integer hotScore;
}
