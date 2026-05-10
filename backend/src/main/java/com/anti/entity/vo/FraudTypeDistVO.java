package com.anti.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 诈骗类型分布VO
 */
@Data
public class FraudTypeDistVO {

    /**
     * 类型名称列表
     */
    private List<String> types;

    /**
     * 浏览量列表
     */
    private List<Integer> counts;
}
