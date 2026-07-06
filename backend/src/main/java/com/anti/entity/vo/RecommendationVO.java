package com.anti.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 推荐结果VO
 */
@Data
public class RecommendationVO {

    /**
     * 推荐项ID
     */
    private Long itemId;

    /**
     * 推荐项类型: case/news/challenge
     */
    private String itemType;

    /**
     * 推荐项子类型，例如 challenge 的 quiz/scenario/agent_scenario
     */
    private String itemSubtype;

    /**
     * 标题
     */
    private String title;

    /**
     * 封面图
     */
    private String coverImage;

    /**
     * 摘要/描述
     */
    private String summary;

    /**
     * 推荐得分
     */
    private BigDecimal score;

    /**
     * 推荐原因列表
     */
    private List<String> reasons;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
