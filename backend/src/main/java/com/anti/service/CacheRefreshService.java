package com.anti.service;

import com.anti.entity.CaseLike;
import com.anti.entity.NewsLike;

/**
 * 缓存刷新服务接口
 * 定义缓存主动刷新和失效策略
 */
public interface CacheRefreshService {

    /**
     * 资讯相关缓存刷新
     */
    void refreshNewsCache(Long newsId);

    /**
     * 案例相关缓存刷新
     */
    void refreshCaseCache(Long caseId);

    /**
     * 用户行为变化时刷新缓存
     */
    void refreshUserCache(Long userId);

    /**
     * 排行榜更新时刷新缓存
     */
    void refreshLeaderboardCache();

    /**
     * 推荐结果失效
     */
    void invalidateRecommendationCache(Long userId);

    /**
     * 统计缓存刷新
     */
    void refreshStatisticsCache();

    /**
     * 点赞事件处理(异步刷新相关缓存)
     */
    void handleLikeEvent(Long userId, String itemType, Long itemId);

    /**
     * 浏览事件处理(异步更新热点数据)
     */
    void handleBrowseEvent(Long userId, String itemType, Long itemId);

    /**
     * 内容发布事件处理
     */
    void handlePublishEvent(String itemType, Long itemId);

    /**
     * 内容更新事件处理
     */
    void handleUpdateEvent(String itemType, Long itemId);

    /**
     * 内容删除事件处理
     */
    void handleDeleteEvent(String itemType, Long itemId);
}
