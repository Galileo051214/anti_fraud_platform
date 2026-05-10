package com.anti.service;

import com.anti.entity.vo.LeaderboardVO;

import java.util.List;

/**
 * 排行榜服务接口
 */
public interface LeaderboardService {

    /**
     * 获取日排行榜
     */
    List<LeaderboardVO> getDailyLeaderboard(Long userId, int limit);

    /**
     * 获取周排行榜
     */
    List<LeaderboardVO> getWeeklyLeaderboard(Long userId, int limit);

    /**
     * 获取总排行榜
     */
    List<LeaderboardVO> getAllTimeLeaderboard(Long userId, int limit);

    /**
     * 获取用户排名
     */
    LeaderboardVO getUserRank(Long userId, String periodType);

    /**
     * 更新用户积分(通关奖励时调用)
     */
    void updateScore(Long userId, int score, String periodType);

    /**
     * 初始化用户排行榜记录
     */
    void initUserLeaderboard(Long userId);

    /**
     * 刷新排行榜排名
     */
    void refreshRankings();
}
