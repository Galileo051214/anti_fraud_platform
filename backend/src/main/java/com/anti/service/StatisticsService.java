package com.anti.service;

import com.anti.entity.vo.*;

/**
 * 统计服务接口
 */
public interface StatisticsService {

    /**
     * 获取管理后台看板数据
     */
    DashboardVO getDashboardData();

    /**
     * 获取访问量趋势
     */
    VisitTrendVO getVisitTrend(int days);

    /**
     * 获取诈骗类型分布
     */
    FraudTypeDistVO getFraudTypeDistribution();

    /**
     * 获取高频诈骗类型TOP N
     */
    java.util.List<java.util.Map<String, Object>> getTopFraudTypes(int limit);

    /**
     * 获取各院系测试得分统计
     */
    DepartmentScoreVO getDepartmentScores();

    /**
     * 获取学生学习完成率统计
     */
    java.util.List<java.util.Map<String, Object>> getCompletionRate();

    /**
     * 获取TOP案例排行榜
     */
    java.util.List<TopCaseVO> getTopCases(int limit);

    /**
     * 获取用户活跃度热力图数据
     */
    java.util.List<HourlyActivityVO> getHourlyActivity(String statDate);

    /**
     * 手动触发统计数据更新
     */
    void triggerStatisticsUpdate();
}
