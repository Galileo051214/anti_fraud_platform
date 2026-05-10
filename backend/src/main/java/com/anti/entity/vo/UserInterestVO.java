package com.anti.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户兴趣分析VO
 */
@Data
public class UserInterestVO {

    /**
     * 生命周期阶段
     */
    private String lifecycleStage;

    /**
     * 阶段名称
     */
    private String lifecycleStageName;

    /**
     * 知识水平(0-100)
     */
    private Integer knowledgeLevel;

    /**
     * 兴趣标签及得分
     */
    private List<TagScore> interestTags;

    /**
     * 弱点标签
     */
    private List<String> weakPoints;

    /**
     * 身份匹配度
     */
    private BigDecimal identityMatchScore;

    /**
     * 标签得分对
     */
    @Data
    public static class TagScore {
        private Long tagId;
        private String tagName;
        private BigDecimal score;
    }
}
