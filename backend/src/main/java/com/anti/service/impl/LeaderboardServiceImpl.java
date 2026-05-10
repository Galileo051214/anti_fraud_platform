package com.anti.service.impl;

import com.anti.common.CacheConstants;
import com.anti.entity.Leaderboard;
import com.anti.entity.vo.LeaderboardVO;
import com.anti.mapper.LeaderboardMapper;
import com.anti.service.LeaderboardService;
import com.anti.util.RedisCacheUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 排行榜服务实现类
 * 集成Redis缓存优化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private final LeaderboardMapper leaderboardMapper;
    private final RedisCacheUtil redisCacheUtil;

    private static final int DEFAULT_CACHE_TTL = 300; // 5分钟

    @Override
    public List<LeaderboardVO> getDailyLeaderboard(Long userId, int limit) {
        // 尝试从缓存获取
        String cacheKey = CacheConstants.getDailyLeaderboardKey(limit);
        Object cached = redisCacheUtil.get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取日排行榜, limit={}", limit);
            @SuppressWarnings("unchecked")
            List<LeaderboardVO> result = (List<LeaderboardVO>) cached;
            return markCurrentUser(result, userId);
        }

        // 缓存未命中，从数据库获取
        log.debug("缓存未命中,从数据库获取日排行榜, limit={}", limit);
        List<Leaderboard> list = leaderboardMapper.selectDailyTop(limit);
        List<LeaderboardVO> result = convertToVOList(list, userId, "daily");
        
        // 写入缓存
        redisCacheUtil.set(cacheKey, result, DEFAULT_CACHE_TTL);
        return result;
    }

    @Override
    public List<LeaderboardVO> getWeeklyLeaderboard(Long userId, int limit) {
        // 尝试从缓存获取
        String cacheKey = CacheConstants.getWeeklyLeaderboardKey(limit);
        Object cached = redisCacheUtil.get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取周排行榜, limit={}", limit);
            @SuppressWarnings("unchecked")
            List<LeaderboardVO> result = (List<LeaderboardVO>) cached;
            return markCurrentUser(result, userId);
        }

        // 缓存未命中，从数据库获取
        log.debug("缓存未命中,从数据库获取周排行榜, limit={}", limit);
        List<Leaderboard> list = leaderboardMapper.selectWeeklyTop(limit);
        List<LeaderboardVO> result = convertToVOList(list, userId, "weekly");
        
        // 写入缓存
        redisCacheUtil.set(cacheKey, result, (int) CacheConstants.WEEKLY_LEADERBOARD_CACHE_TTL);
        return result;
    }

    @Override
    public List<LeaderboardVO> getAllTimeLeaderboard(Long userId, int limit) {
        // 尝试从缓存获取
        String cacheKey = CacheConstants.getAllTimeLeaderboardKey(limit);
        Object cached = redisCacheUtil.get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取总排行榜, limit={}", limit);
            @SuppressWarnings("unchecked")
            List<LeaderboardVO> result = (List<LeaderboardVO>) cached;
            return markCurrentUser(result, userId);
        }

        // 缓存未命中，从数据库获取
        log.debug("缓存未命中,从数据库获取总排行榜, limit={}", limit);
        List<Leaderboard> list = leaderboardMapper.selectAllTimeTop(limit);
        List<LeaderboardVO> result = convertToVOList(list, userId, "all");
        
        // 写入缓存
        redisCacheUtil.set(cacheKey, result, (int) CacheConstants.ALLTIME_LEADERBOARD_CACHE_TTL);
        return result;
    }

    @Override
    public LeaderboardVO getUserRank(Long userId, String periodType) {
        // 尝试从缓存获取
        String cacheKey = CacheConstants.getUserRankKey(userId, periodType);
        Object cached = redisCacheUtil.get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取用户排名, userId={}, period={}", userId, periodType);
            return (LeaderboardVO) cached;
        }

        // 缓存未命中，从数据库获取
        Leaderboard record = leaderboardMapper.selectUserRank(userId, periodType);
        LeaderboardVO vo;
        if (record == null) {
            vo = new LeaderboardVO();
            vo.setUserId(userId);
            vo.setRank(0);
            vo.setScore(0);
            vo.setPeriodType(periodType);
            vo.setIsCurrentUser(true);
        } else {
            vo = new LeaderboardVO();
            vo.setRank(record.getRank());
            vo.setUserId(record.getUserId());
            vo.setScore(record.getScore());
            vo.setPeriodType(record.getPeriodType());
            vo.setIsCurrentUser(true);
        }
        
        // 写入缓存
        redisCacheUtil.set(cacheKey, vo, DEFAULT_CACHE_TTL);
        return vo;
    }

    @Override
    @Transactional
    public void updateScore(Long userId, int score, String periodType) {
        Leaderboard record = leaderboardMapper.selectUserRank(userId, periodType);
        if (record == null) {
            initUserLeaderboard(userId);
            record = leaderboardMapper.selectUserRank(userId, periodType);
        }

        if (record != null) {
            record.setScore(record.getScore() + score);
            leaderboardMapper.updateById(record);
        }

        // 刷新排行榜缓存
        refreshRankings();
        
        // 清除用户排名缓存
        redisCacheUtil.delete(CacheConstants.getUserRankKey(userId, periodType));
    }

    @Override
    @Transactional
    public void initUserLeaderboard(Long userId) {
        String[] types = {"daily", "weekly", "all"};
        LocalDate today = LocalDate.now();

        for (String type : types) {
            Leaderboard existing = leaderboardMapper.selectUserRank(userId, type);
            if (existing == null) {
                Leaderboard record = new Leaderboard();
                record.setUserId(userId);
                record.setPeriodType(type);
                record.setScore(0);
                record.setRank(0);
                record.setUpdateDate(today);
                leaderboardMapper.insert(record);
            }
        }
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?") // 每天凌晨执行
    @Transactional
    public void refreshRankings() {
        log.info("开始刷新排行榜排名...");
        refreshDailyRankings();
        refreshPeriodRankings("weekly");
        refreshPeriodRankings("all");
        
        // 清除所有排行榜缓存
        redisCacheUtil.deleteAllLeaderboard();
        log.info("排行榜排名刷新完成");
    }

    private void refreshDailyRankings() {
        LambdaQueryWrapper<Leaderboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Leaderboard::getPeriodType, "daily")
                .eq(Leaderboard::getUpdateDate, LocalDate.now())
                .orderByDesc(Leaderboard::getScore);

        List<Leaderboard> records = leaderboardMapper.selectList(wrapper);
        updateRanks(records);

        LambdaQueryWrapper<Leaderboard> oldWrapper = new LambdaQueryWrapper<>();
        oldWrapper.eq(Leaderboard::getPeriodType, "daily")
                .ne(Leaderboard::getUpdateDate, LocalDate.now());
        List<Leaderboard> oldRecords = leaderboardMapper.selectList(oldWrapper);
        for (Leaderboard r : oldRecords) {
            r.setScore(0);
            r.setUpdateDate(LocalDate.now());
            leaderboardMapper.updateById(r);
        }
    }

    private void refreshPeriodRankings(String periodType) {
        LambdaQueryWrapper<Leaderboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Leaderboard::getPeriodType, periodType)
                .orderByDesc(Leaderboard::getScore);

        List<Leaderboard> records = leaderboardMapper.selectList(wrapper);
        updateRanks(records);
    }

    private void updateRanks(List<Leaderboard> records) {
        int rank = 1;
        for (Leaderboard record : records) {
            record.setRank(rank++);
            leaderboardMapper.updateById(record);
        }
    }

    private List<LeaderboardVO> convertToVOList(List<Leaderboard> list, Long currentUserId, String periodType) {
        return list.stream().map(record -> {
            LeaderboardVO vo = new LeaderboardVO();
            vo.setRank(record.getRank());
            vo.setUserId(record.getUserId());
            vo.setScore(record.getScore());
            vo.setPeriodType(periodType);
            vo.setIsCurrentUser(record.getUserId().equals(currentUserId));
            return vo;
        }).collect(Collectors.toList());
    }
    
    private List<LeaderboardVO> markCurrentUser(List<LeaderboardVO> list, Long currentUserId) {
        if (currentUserId == null) {
            return list;
        }
        return list.stream().map(vo -> {
            vo.setIsCurrentUser(vo.getUserId().equals(currentUserId));
            return vo;
        }).collect(Collectors.toList());
    }
}
