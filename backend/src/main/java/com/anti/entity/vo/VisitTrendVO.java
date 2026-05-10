package com.anti.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 访问量趋势VO
 */
@Data
public class VisitTrendVO {

    /**
     * 日期列表
     */
    private List<String> dates;

    /**
     * 页面浏览量列表
     */
    private List<Integer> pageViews;

    /**
     * 日活用户数列表
     */
    private List<Integer> activeUsers;

    /**
     * 新增用户数列表
     */
    private List<Integer> newUsers;
}
