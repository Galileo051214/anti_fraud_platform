package com.anti.service.impl;

import com.anti.common.CacheConstants;
import com.anti.entity.News;
import com.anti.entity.vo.LeaderboardVO;
import com.anti.mapper.NewsMapper;
import com.anti.service.CacheService;
import com.anti.service.FraudCaseService;
import com.anti.service.LeaderboardService;
import com.anti.util.RedisCacheUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 缓存服务实现类
 * 实现各类缓存操作的统一管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final RedisCacheUtil redisCacheUtil;
    private final NewsMapper newsMapper;
    private final FraudCaseService fraudCaseService;
    private final LeaderboardService leaderboardService;

    // 紧急预警内存缓存(实际项目中可使用Redis List)
    private final List<Object> emergencyAlerts = new CopyOnWriteArrayList<>();

    // ==================== 热点资讯缓存 ====================

    @Override
    @SuppressWarnings("unchecked")
    public List<News> getHotNews(int limit) {
        Object cached = redisCacheUtil.getHotNews(limit);
        if (cached != null) {
            log.debug("从缓存获取热点资讯, limit={}", limit);
            return (List<News>) cached;
        }

        log.debug("缓存未命中,从数据库获取热点资讯, limit={}", limit);
        List<News> hotNews = fetchHotNewsFromDatabase(limit);
        redisCacheUtil.setHotNews(limit, hotNews);
        return hotNews;
    }

    @Override
    @Scheduled(fixedRate = 300000) // 每5分钟刷新
    public void refreshHotNewsCache() {
        log.info("刷新热点资讯缓存...");
        try {
            // 刷新不同数量的缓存
            int[] limits = {10, 20, 50};
            for (int limit : limits) {
                List<News> hotNews = fetchHotNewsFromDatabase(limit);
                redisCacheUtil.setHotNews(limit, hotNews);
            }
            log.info("热点资讯缓存刷新完成");
        } catch (Exception e) {
            log.error("刷新热点资讯缓存失败", e);
        }
    }

    private List<News> fetchHotNewsFromDatabase(int limit) {
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(News::getStatus, 1)
               .orderByDesc(News::getIsTop)
               .orderByDesc(News::getViewCount)
               .orderByDesc(News::getPublishTime)
               .last("LIMIT " + limit);
        return newsMapper.selectList(wrapper);
    }

    // ==================== 热点案例缓存 ====================

    @Override
    @SuppressWarnings("unchecked")
    public List<?> getHotCases(int limit) {
        Object cached = redisCacheUtil.getHotCases(limit);
        if (cached != null) {
            log.debug("从缓存获取热点案例, limit={}", limit);
            return (List<?>) cached;
        }

        log.debug("缓存未命中,从数据库获取热点案例, limit={}", limit);
        List<?> hotCases = fraudCaseService.getHotCases(limit);
        redisCacheUtil.setHotCases(limit, hotCases);
        return hotCases;
    }

    @Override
    @Scheduled(fixedRate = 300000) // 每5分钟刷新
    public void refreshHotCasesCache() {
        log.info("刷新热点案例缓存...");
        try {
            int[] limits = {10, 20, 50};
            for (int limit : limits) {
                List<?> hotCases = fraudCaseService.getHotCases(limit);
                redisCacheUtil.setHotCases(limit, hotCases);
            }
            log.info("热点案例缓存刷新完成");
        } catch (Exception e) {
            log.error("刷新热点案例缓存失败", e);
        }
    }

    // ==================== 紧急预警缓存 ====================

    @Override
    @SuppressWarnings("unchecked")
    public List<?> getEmergencyAlerts(String alertType) {
        Object cached = redisCacheUtil.getEmergencyAlert(alertType);
        if (cached != null) {
            log.debug("从缓存获取紧急预警, type={}", alertType);
            return (List<?>) cached;
        }

        // 从内存缓存获取
        List<Object> alerts = filterAlertsByType(alertType);
        redisCacheUtil.setEmergencyAlert(alertType, alerts);
        return alerts;
    }

    @Override
    public void publishEmergencyAlert(String alertType, Object alert) {
        log.info("发布紧急预警, type={}", alertType);
        emergencyAlerts.add(alert);
        // 刷新缓存
        redisCacheUtil.setEmergencyAlert(alertType, filterAlertsByType(alertType));
        // 刷新全部类型缓存
        redisCacheUtil.setEmergencyAlert("all", new ArrayList<>(emergencyAlerts));
    }

    @Override
    public void clearEmergencyAlerts() {
        log.info("清除所有紧急预警");
        emergencyAlerts.clear();
        redisCacheUtil.deleteEmergencyAlert();
    }

    private List<Object> filterAlertsByType(String alertType) {
        if (alertType == null || "all".equals(alertType)) {
            return new ArrayList<>(emergencyAlerts);
        }
        List<Object> filtered = new ArrayList<>();
        for (Object alert : emergencyAlerts) {
            // 实际项目中根据alert对象的类型字段过滤
            filtered.add(alert);
        }
        return filtered;
    }

    // ==================== 排行榜缓存 ====================

    @Override
    @SuppressWarnings("unchecked")
    public List<LeaderboardVO> getDailyLeaderboard(Long userId, int limit) {
        Object cached = redisCacheUtil.getDailyLeaderboard(limit);
        if (cached != null) {
            log.debug("从缓存获取日排行榜, limit={}", limit);
            return (List<LeaderboardVO>) cached;
        }

        log.debug("缓存未命中,从数据库获取日排行榜, limit={}", limit);
        List<LeaderboardVO> leaderboard = leaderboardService.getDailyLeaderboard(userId, limit);
        redisCacheUtil.setDailyLeaderboard(limit, leaderboard);
        return leaderboard;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LeaderboardVO> getWeeklyLeaderboard(Long userId, int limit) {
        Object cached = redisCacheUtil.getWeeklyLeaderboard(limit);
        if (cached != null) {
            log.debug("从缓存获取周排行榜, limit={}", limit);
            return (List<LeaderboardVO>) cached;
        }

        log.debug("缓存未命中,从数据库获取周排行榜, limit={}", limit);
        List<LeaderboardVO> leaderboard = leaderboardService.getWeeklyLeaderboard(userId, limit);
        redisCacheUtil.setWeeklyLeaderboard(limit, leaderboard);
        return leaderboard;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LeaderboardVO> getAllTimeLeaderboard(Long userId, int limit) {
        Object cached = redisCacheUtil.getAllTimeLeaderboard(limit);
        if (cached != null) {
            log.debug("从缓存获取总排行榜, limit={}", limit);
            return (List<LeaderboardVO>) cached;
        }

        log.debug("缓存未命中,从数据库获取总排行榜, limit={}", limit);
        List<LeaderboardVO> leaderboard = leaderboardService.getAllTimeLeaderboard(userId, limit);
        redisCacheUtil.setAllTimeLeaderboard(limit, leaderboard);
        return leaderboard;
    }

    @Override
    public void refreshLeaderboardCache() {
        log.info("刷新排行榜缓存...");
        try {
            // 触发排行榜重新计算
            leaderboardService.refreshRankings();
            
            // 刷新缓存
            int[] limits = {10, 50, 100};
            for (int limit : limits) {
                List<LeaderboardVO> daily = leaderboardService.getDailyLeaderboard(null, limit);
                redisCacheUtil.setDailyLeaderboard(limit, daily);
                
                List<LeaderboardVO> weekly = leaderboardService.getWeeklyLeaderboard(null, limit);
                redisCacheUtil.setWeeklyLeaderboard(limit, weekly);
                
                List<LeaderboardVO> allTime = leaderboardService.getAllTimeLeaderboard(null, limit);
                redisCacheUtil.setAllTimeLeaderboard(limit, allTime);
            }
            log.info("排行榜缓存刷新完成");
        } catch (Exception e) {
            log.error("刷新排行榜缓存失败", e);
        }
    }

    // ==================== 通用缓存操作 ====================

    @Override
    public void clearAllCache() {
        log.warn("清除所有应用缓存...");
        
        // 清除热点缓存
        redisCacheUtil.deleteHotNews();
        redisCacheUtil.deleteHotCases();
        
        // 清除紧急预警缓存
        redisCacheUtil.deleteEmergencyAlert();
        
        // 清除排行榜缓存
        redisCacheUtil.deleteAllLeaderboard();
        
        // 清除推荐缓存
        redisCacheUtil.deleteAllRecommendations();
        
        // 清除统计缓存
        redisCacheUtil.deleteAllStatistics();
        
        // 清除紧急预警内存缓存
        emergencyAlerts.clear();
        
        log.info("所有应用缓存已清除");
    }

    @Override
    @Scheduled(fixedRate = 3600000) // 每小时预热
    public void warmupHotDataCache() {
        log.info("开始预热热点数据缓存...");
        try {
            // 预热热点资讯
            refreshHotNewsCache();
            
            // 预热热点案例
            refreshHotCasesCache();
            
            // 预热排行榜
            refreshLeaderboardCache();
            
            log.info("热点数据缓存预热完成");
        } catch (Exception e) {
            log.error("预热热点数据缓存失败", e);
        }
    }
}
