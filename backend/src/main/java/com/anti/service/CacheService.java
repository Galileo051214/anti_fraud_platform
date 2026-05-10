package com.anti.service;

import com.anti.entity.News;
import com.anti.entity.vo.LeaderboardVO;

import java.util.List;

/**
 * 缓存服务接口
 * 定义各类缓存操作的统一接口
 */
public interface CacheService {

    // ==================== 热点资讯缓存 ====================

    /**
     * 获取热点资讯列表
     * @param limit 返回数量
     * @return 热点资讯列表
     */
    List<News> getHotNews(int limit);

    /**
     * 刷新热点资讯缓存
     */
    void refreshHotNewsCache();

    // ==================== 热点案例缓存 ====================

    /**
     * 获取热点案例列表
     * @param limit 返回数量
     * @return 热点案例列表
     */
    List<?> getHotCases(int limit);

    /**
     * 刷新热点案例缓存
     */
    void refreshHotCasesCache();

    // ==================== 紧急预警缓存 ====================

    /**
     * 获取紧急预警列表
     * @param alertType 预警类型
     * @return 预警列表
     */
    List<?> getEmergencyAlerts(String alertType);

    /**
     * 发布紧急预警(缓存)
     */
    void publishEmergencyAlert(String alertType, Object alert);

    /**
     * 清除紧急预警
     */
    void clearEmergencyAlerts();

    // ==================== 排行榜缓存 ====================

    /**
     * 获取日排行榜
     * @param userId 当前用户ID
     * @param limit 返回数量
     * @return 排行榜列表
     */
    List<LeaderboardVO> getDailyLeaderboard(Long userId, int limit);

    /**
     * 获取周排行榜
     * @param userId 当前用户ID
     * @param limit 返回数量
     * @return 排行榜列表
     */
    List<LeaderboardVO> getWeeklyLeaderboard(Long userId, int limit);

    /**
     * 获取总排行榜
     * @param userId 当前用户ID
     * @param limit 返回数量
     * @return 排行榜列表
     */
    List<LeaderboardVO> getAllTimeLeaderboard(Long userId, int limit);

    /**
     * 刷新排行榜缓存
     */
    void refreshLeaderboardCache();

    // ==================== 通用缓存操作 ====================

    /**
     * 清除所有缓存
     */
    void clearAllCache();

    /**
     * 预热热点数据缓存
     */
    void warmupHotDataCache();
}
