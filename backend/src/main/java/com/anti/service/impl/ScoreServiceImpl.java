package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.UserScore;
import com.anti.mapper.UserScoreMapper;
import com.anti.service.ScoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private final UserScoreMapper scoreMapper;

    private static final int LEVEL_THRESHOLD = 100;

    @Override
    public UserScore getScoreByUserId(Long userId) {
        UserScore score = scoreMapper.selectByUserId(userId);
        if (score == null) {
            throw new BusinessException(404, "用户积分记录不存在");
        }
        return score;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addScore(Long userId, Integer score, String reason) {
        if (score == null || score <= 0) {
            throw new BusinessException(400, "积分必须大于0");
        }
        // 保证用户积分记录存在：否则首次升级/首次通关时会触发 NPE
        UserScore userScore = scoreMapper.selectByUserId(userId);
        if (userScore == null) {
            initScore(userId);
            userScore = scoreMapper.selectByUserId(userId);
        }

        scoreMapper.updateScore(userId, score);
        userScore = scoreMapper.selectByUserId(userId);
        int newLevel = calculateLevel(userScore.getTotalScore());
        if (newLevel > userScore.getCurrentLevel()) {
            userScore.setCurrentLevel(newLevel);
            scoreMapper.updateById(userScore);
            log.info("用户 {} 升级到等级 {}", userId, newLevel);
        }
        log.info("用户 {} 增加 {} 积分，原因：{}", userId, score, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductScore(Long userId, Integer score, String reason) {
        if (score == null || score <= 0) {
            throw new BusinessException(400, "扣减积分必须大于0");
        }
        UserScore userScore = scoreMapper.selectByUserId(userId);
        if (userScore == null) {
            throw new BusinessException(404, "用户积分记录不存在");
        }
        if (userScore.getTotalScore() < score) {
            throw new BusinessException(400, "积分不足，无法扣减");
        }
        scoreMapper.updateScore(userId, -score);
        log.info("用户 {} 扣减 {} 积分，原因：{}", userId, score, reason);
    }

    @Override
    public int calculateLevel(Integer totalScore) {
        if (totalScore == null || totalScore < 0) {
            return 1;
        }
        return (totalScore / LEVEL_THRESHOLD) + 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initScore(Long userId) {
        UserScore existingScore = scoreMapper.selectByUserId(userId);
        if (existingScore != null) {
            return;
        }
        UserScore score = new UserScore();
        score.setUserId(userId);
        score.setTotalScore(0);
        score.setCurrentLevel(1);
        score.setWeeklyScore(0);
        score.setUpdateTime(LocalDateTime.now());
        scoreMapper.insert(score);
        log.info("初始化用户 {} 积分记录", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetWeeklyScore(Long userId) {
        UserScore score = scoreMapper.selectByUserId(userId);
        if (score != null) {
            score.setWeeklyScore(0);
            scoreMapper.updateById(score);
            log.info("重置用户 {} 本周积分", userId);
        }
    }
}
