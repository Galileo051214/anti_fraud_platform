package com.anti.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 推荐结果请求DTO
 */
@Data
public class RecommendationRequest {

    /**
     * 推荐数量
     */
    private Integer limit = 10;

    /**
     * 推荐类型(可选: case/news/challenge)
     * 默认全部
     */
    private String itemType;

    /**
     * 是否强制刷新(忽略缓存)
     */
    private Boolean forceRefresh = false;
}
