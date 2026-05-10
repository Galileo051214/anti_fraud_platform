package com.anti.service.impl;

import com.anti.common.CacheConstants;
import com.anti.entity.CaseLike;
import com.anti.entity.NewsLike;
import com.anti.mapper.CaseLikeMapper;
import com.anti.mapper.NewsLikeMapper;
import com.anti.service.CacheRefreshService;
import com.anti.service.CacheService;
import com.anti.util.RedisCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 缓存刷新服务实现类
 * 实现缓存主动刷新和失效策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheRefreshServiceImpl implements CacheRefreshService {

    private final RedisCacheUtil redisCacheUtil;
    private final CacheService cacheService;
    private final NewsLikeMapper newsLikeMapper;
    private final CaseLikeMapper caseLikeMapper;

    @Override
    @Async
    public void refreshNewsCache(Long newsId) {
        log.info("刷新资讯缓存, newsId={}", newsId);
        try {
            // 删除资讯详情缓存
            redisCacheUtil.deleteNewsDetail(newsId);
            
            // 刷新热点资讯缓存
            redisCacheUtil.deleteHotNews();
        } catch (Exception e) {
            log.error("刷新资讯缓存失败, newsId={}", newsId, e);
        }
    }

    @Override
    @Async
    public void refreshCaseCache(Long caseId) {
        log.info("刷新案例缓存, caseId={}", caseId);
        try {
            // 删除案例详情缓存
            redisCacheUtil.deleteCaseDetail(caseId);
            
            // 刷新热点案例缓存
            redisCacheUtil.deleteHotCases();
            
            // 刷新推荐缓存(可能包含该案例)
            redisCacheUtil.deleteAllRecommendations();
        } catch (Exception e) {
            log.error("刷新案例缓存失败, caseId={}", caseId, e);
        }
    }

    @Override
    @Async
    public void refreshUserCache(Long userId) {
        log.info("刷新用户缓存, userId={}", userId);
        try {
            // 删除用户会话缓存
            redisCacheUtil.deleteUserSession(userId);
            
            // 删除用户兴趣缓存
            redisCacheUtil.deleteUserInterest(userId);
            
            // 删除用户推荐缓存
            redisCacheUtil.deleteUserRecommendations(userId);
            
            // 删除用户画像缓存
            redisCacheUtil.delete(CacheConstants.getUserProfileKey(userId));
        } catch (Exception e) {
            log.error("刷新用户缓存失败, userId={}", userId, e);
        }
    }

    @Override
    @Async
    public void refreshLeaderboardCache() {
        log.info("刷新排行榜缓存");
        try {
            redisCacheUtil.deleteAllLeaderboard();
        } catch (Exception e) {
            log.error("刷新排行榜缓存失败", e);
        }
    }

    @Override
    @Async
    public void invalidateRecommendationCache(Long userId) {
        log.info("失效推荐缓存, userId={}", userId);
        try {
            if (userId != null) {
                redisCacheUtil.deleteUserRecommendations(userId);
                redisCacheUtil.deleteUserInterest(userId);
            } else {
                redisCacheUtil.deleteAllRecommendations();
            }
        } catch (Exception e) {
            log.error("失效推荐缓存失败, userId={}", userId, e);
        }
    }

    @Override
    @Async
    public void refreshStatisticsCache() {
        log.info("刷新统计数据缓存");
        try {
            redisCacheUtil.deleteAllStatistics();
        } catch (Exception e) {
            log.error("刷新统计数据缓存失败", e);
        }
    }

    @Override
    @Async
    public void handleLikeEvent(Long userId, String itemType, Long itemId) {
        log.info("处理点赞事件, userId={}, type={}, id={}", userId, itemType, itemId);
        try {
            switch (itemType.toLowerCase()) {
                case "news":
                    handleNewsLike(itemId);
                    break;
                case "case":
                case "fraudcase":
                    handleCaseLike(itemId);
                    break;
                default:
                    log.warn("未知的点赞类型: {}", itemType);
            }
        } catch (Exception e) {
            log.error("处理点赞事件失败, userId={}, type={}, id={}", userId, itemType, itemId, e);
        }
    }

    private void handleNewsLike(Long newsId) {
        // 资讯点赞数变化，只影响详情页缓存和统计缓存
        redisCacheUtil.deleteNewsDetail(newsId);
        refreshStatisticsCache();
    }

    private void handleCaseLike(Long caseId) {
        // 案例点赞数变化，影响案例缓存和热点缓存
        redisCacheUtil.deleteCaseDetail(caseId);
        redisCacheUtil.deleteHotCases();
        redisCacheUtil.deleteAllRecommendations();
    }

    @Override
    @Async
    public void handleBrowseEvent(Long userId, String itemType, Long itemId) {
        log.info("处理浏览事件, userId={}, type={}, id={}", userId, itemType, itemId);
        try {
            switch (itemType.toLowerCase()) {
                case "news":
                    redisCacheUtil.deleteNewsDetail(itemId);
                    redisCacheUtil.deleteHotNews();
                    break;
                case "case":
                case "fraudcase":
                    redisCacheUtil.deleteCaseDetail(itemId);
                    redisCacheUtil.deleteHotCases();
                    // 用户浏览行为可能影响推荐，重新加载用户兴趣
                    if (userId != null) {
                        redisCacheUtil.deleteUserInterest(userId);
                    }
                    break;
                case "challenge":
                    // 闯关完成后可能影响排行榜
                    refreshLeaderboardCache();
                    break;
                default:
                    log.warn("未知的浏览类型: {}", itemType);
            }
        } catch (Exception e) {
            log.error("处理浏览事件失败, userId={}, type={}, id={}", userId, itemType, itemId, e);
        }
    }

    @Override
    @Async
    public void handlePublishEvent(String itemType, Long itemId) {
        log.info("处理发布事件, type={}, id={}", itemType, itemId);
        try {
            switch (itemType.toLowerCase()) {
                case "news":
                    redisCacheUtil.deleteHotNews();
                    redisCacheUtil.deleteAllStatistics();
                    break;
                case "case":
                case "fraudcase":
                    redisCacheUtil.deleteHotCases();
                    redisCacheUtil.deleteAllStatistics();
                    break;
                default:
                    log.warn("未知的发布类型: {}", itemType);
            }
        } catch (Exception e) {
            log.error("处理发布事件失败, type={}, id={}", itemType, itemId, e);
        }
    }

    @Override
    @Async
    public void handleUpdateEvent(String itemType, Long itemId) {
        log.info("处理更新事件, type={}, id={}", itemType, itemId);
        try {
            switch (itemType.toLowerCase()) {
                case "news":
                    redisCacheUtil.deleteNewsDetail(itemId);
                    redisCacheUtil.deleteHotNews();
                    break;
                case "case":
                case "fraudcase":
                    redisCacheUtil.deleteCaseDetail(itemId);
                    redisCacheUtil.deleteHotCases();
                    break;
                case "user":
                    // 用户信息更新
                    if (itemId != null) {
                        refreshUserCache(itemId);
                    }
                    break;
                default:
                    log.warn("未知的更新类型: {}", itemType);
            }
        } catch (Exception e) {
            log.error("处理更新事件失败, type={}, id={}", itemType, itemId, e);
        }
    }

    @Override
    @Async
    public void handleDeleteEvent(String itemType, Long itemId) {
        log.info("处理删除事件, type={}, id={}", itemType, itemId);
        try {
            switch (itemType.toLowerCase()) {
                case "news":
                    redisCacheUtil.deleteNewsDetail(itemId);
                    redisCacheUtil.deleteHotNews();
                    redisCacheUtil.deleteAllStatistics();
                    break;
                case "case":
                case "fraudcase":
                    redisCacheUtil.deleteCaseDetail(itemId);
                    redisCacheUtil.deleteHotCases();
                    redisCacheUtil.deleteAllStatistics();
                    redisCacheUtil.deleteAllRecommendations();
                    break;
                case "user":
                    if (itemId != null) {
                        refreshUserCache(itemId);
                    }
                    break;
                default:
                    log.warn("未知的删除类型: {}", itemType);
            }
        } catch (Exception e) {
            log.error("处理删除事件失败, type={}, id={}", itemType, itemId, e);
        }
    }
}
