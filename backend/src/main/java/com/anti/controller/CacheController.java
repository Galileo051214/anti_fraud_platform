package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.News;
import com.anti.entity.vo.LeaderboardVO;
import com.anti.service.CacheRefreshService;
import com.anti.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 缓存管理控制器
 * 提供缓存查询、手动刷新、失效等管理接口
 */
@Tag(name = "缓存管理", description = "系统缓存管理接口")
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;
    private final CacheRefreshService cacheRefreshService;

    @Operation(summary = "获取热点资讯", description = "获取缓存的热点资讯列表")
    @GetMapping("/hot-news")
    public Result<List<News>> getHotNews(
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") int limit) {
        List<News> hotNews = cacheService.getHotNews(limit);
        return Result.success(hotNews);
    }

    @Operation(summary = "获取热点案例", description = "获取缓存的热点案例列表")
    @GetMapping("/hot-cases")
    public Result<List<?>> getHotCases(
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") int limit) {
        List<?> hotCases = cacheService.getHotCases(limit);
        return Result.success(hotCases);
    }

    @Operation(summary = "获取紧急预警", description = "获取当前紧急预警列表")
    @GetMapping("/emergency-alerts")
    public Result<List<?>> getEmergencyAlerts(
            @Parameter(description = "预警类型") @RequestParam(required = false) String alertType) {
        List<?> alerts = cacheService.getEmergencyAlerts(alertType);
        return Result.success(alerts);
    }

    @Operation(summary = "发布紧急预警", description = "管理员发布紧急预警消息")
    @PostMapping("/emergency-alert")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> publishEmergencyAlert(
            @RequestBody Map<String, Object> alertData) {
        String alertType = (String) alertData.getOrDefault("type", "all");
        cacheService.publishEmergencyAlert(alertType, alertData);
        return Result.success(null);
    }

    @Operation(summary = "清除紧急预警", description = "管理员清除所有紧急预警")
    @DeleteMapping("/emergency-alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> clearEmergencyAlerts() {
        cacheService.clearEmergencyAlerts();
        return Result.success(null);
    }

    @Operation(summary = "获取日排行榜", description = "获取缓存的日排行榜")
    @GetMapping("/leaderboard/daily")
    public Result<List<LeaderboardVO>> getDailyLeaderboard(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        List<LeaderboardVO> leaderboard = cacheService.getDailyLeaderboard(userId, limit);
        return Result.success(leaderboard);
    }

    @Operation(summary = "获取周排行榜", description = "获取缓存的周排行榜")
    @GetMapping("/leaderboard/weekly")
    public Result<List<LeaderboardVO>> getWeeklyLeaderboard(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        List<LeaderboardVO> leaderboard = cacheService.getWeeklyLeaderboard(userId, limit);
        return Result.success(leaderboard);
    }

    @Operation(summary = "获取总排行榜", description = "获取缓存的总排行榜")
    @GetMapping("/leaderboard/all")
    public Result<List<LeaderboardVO>> getAllTimeLeaderboard(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        List<LeaderboardVO> leaderboard = cacheService.getAllTimeLeaderboard(userId, limit);
        return Result.success(leaderboard);
    }

    @Operation(summary = "手动刷新热点资讯缓存", description = "管理员手动刷新热点资讯缓存")
    @PostMapping("/refresh/hot-news")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> refreshHotNewsCache() {
        cacheService.refreshHotNewsCache();
        return Result.success(null);
    }

    @Operation(summary = "手动刷新热点案例缓存", description = "管理员手动刷新热点案例缓存")
    @PostMapping("/refresh/hot-cases")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> refreshHotCasesCache() {
        cacheService.refreshHotCasesCache();
        return Result.success(null);
    }

    @Operation(summary = "手动刷新排行榜缓存", description = "管理员手动刷新排行榜缓存")
    @PostMapping("/refresh/leaderboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> refreshLeaderboardCache() {
        cacheService.refreshLeaderboardCache();
        return Result.success(null);
    }

    @Operation(summary = "预热热点数据缓存", description = "管理员触发热点数据缓存预热")
    @PostMapping("/warmup")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> warmupHotDataCache() {
        cacheService.warmupHotDataCache();
        return Result.success(null);
    }

    @Operation(summary = "清除所有应用缓存", description = "管理员清除所有应用缓存(危险操作)")
    @DeleteMapping("/clear-all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> clearAllCache() {
        cacheService.clearAllCache();
        return Result.success(null);
    }

    @Operation(summary = "失效资讯相关缓存", description = "当资讯数据变化时调用")
    @PostMapping("/invalidate/news/{newsId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> invalidateNewsCache(
            @PathVariable Long newsId) {
        cacheRefreshService.refreshNewsCache(newsId);
        return Result.success(null);
    }

    @Operation(summary = "失效案例相关缓存", description = "当案例数据变化时调用")
    @PostMapping("/invalidate/case/{caseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> invalidateCaseCache(
            @PathVariable Long caseId) {
        cacheRefreshService.refreshCaseCache(caseId);
        return Result.success(null);
    }

    @Operation(summary = "失效用户相关缓存", description = "当用户数据变化时调用")
    @PostMapping("/invalidate/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> invalidateUserCache(
            @PathVariable Long userId) {
        cacheRefreshService.refreshUserCache(userId);
        return Result.success(null);
    }

    @Operation(summary = "失效推荐缓存", description = "当推荐数据需要更新时调用")
    @PostMapping("/invalidate/recommendation")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> invalidateRecommendationCache(
            @RequestParam(required = false) Long userId) {
        cacheRefreshService.invalidateRecommendationCache(userId);
        return Result.success(null);
    }

    @Operation(summary = "失效统计缓存", description = "当统计数据需要更新时调用")
    @PostMapping("/invalidate/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> invalidateStatisticsCache() {
        cacheRefreshService.refreshStatisticsCache();
        return Result.success(null);
    }

    @Operation(summary = "获取缓存状态信息", description = "获取当前缓存的使用状态")
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getCacheStatus() {
        // 返回缓存状态信息
        Map<String, Object> status = Map.of(
                "hotNewsCache", "active",
                "hotCasesCache", "active",
                "leaderboardCache", "active",
                "recommendationCache", "active"
        );
        return Result.success(status);
    }

    private Long extractUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                com.anti.security.JwtUtils jwtUtils = new com.anti.security.JwtUtils();
                return jwtUtils.getUserIdFromToken(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
