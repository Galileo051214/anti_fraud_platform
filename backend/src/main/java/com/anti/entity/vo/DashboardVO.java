package com.anti.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 统计看板VO - 用于前端数据展示
 */
@Data
public class DashboardVO {

    /**
     * 今日访问量
     */
    private Integer todayViews;

    /**
     * 今日新增用户
     */
    private Integer todayNewUsers;

    /**
     * 今日活跃用户
     */
    private Integer todayActiveUsers;

    /**
     * 总案例数
     */
    private Integer totalCases;

    /**
     * 总用户数
     */
    private Integer totalUsers;

    /**
     * 今日闯关完成数：当日「用户-关卡」维度下去重后的通关次数（非「完成全部关卡的用户数」）
     */
    private Integer totalChallengeCompletions;

    /**
     * 平均测试得分
     */
    private BigDecimal avgTestScore;

    /**
     * 访问量趋势数据
     */
    private VisitTrendVO visitTrend;

    /**
     * 诈骗类型分布
     */
    private FraudTypeDistVO fraudTypeDist;

    /**
     * 院系得分对比
     */
    private DepartmentScoreVO departmentScores;

    /**
     * TOP案例排行
     */
    private java.util.List<TopCaseVO> topCases;

    /**
     * 用户活跃度热力图数据
     */
    private java.util.List<HourlyActivityVO> hourlyActivity;
}
