package com.anti.service.impl;

import com.anti.entity.Achievement;
import com.anti.entity.UserAchievement;
import com.anti.mapper.AchievementMapper;
import com.anti.mapper.LearningActivityMapper;
import com.anti.mapper.UserAchievementMapper;
import com.anti.service.AchievementService;
import com.anti.service.ScoreService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private static final ZoneId ACTIVITY_ZONE = ZoneId.of("Asia/Shanghai");

    private final AchievementMapper achievementMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final LearningActivityMapper learningActivityMapper;
    private final ScoreService scoreService;

    @Override
    public List<Achievement> getAllAchievements() {
        return achievementMapper.selectAllEnabled();
    }

    @Override
    public List<UserAchievement> getUserAchievements(Long userId) {
        return userAchievementMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlockAchievement(Long userId, String achievementCode) {
        Achievement achievement = achievementMapper.selectByCode(achievementCode);
        if (achievement == null) {
            log.warn("成就代码不存在: {}", achievementCode);
            return false;
        }
        return grantAchievementIfAbsent(userId, achievement);
    }

    /**
     * 仅当本事务内首次插入 user_achievement 成功时发放积分，避免「先查后写」竞态导致重复加分。
     */
    private boolean grantAchievementIfAbsent(Long userId, Achievement achievement) {
        Long achievementId = achievement.getId();
        Integer inserted = userAchievementMapper.insertIfAbsent(userId, achievementId);
        if (inserted == null || inserted == 0) {
            log.debug("用户 {} 成就已存在或插入失败，跳过: {} (inserted={})",
                    userId, achievement.getCode(), inserted);
            return false;
        }
        int reward = achievement.getScoreReward() != null ? achievement.getScoreReward() : 0;
        if (reward > 0) {
            try {
                scoreService.addScore(userId, reward, "解锁成就: " + achievement.getName());
            } catch (Exception e) {
                log.error("用户 {} 解锁成就 {} 成功但积分发放失败: {}",
                        userId, achievement.getName(), e.getMessage());
                throw e;
            }
        }
        log.info("用户 {} 解锁成就 {}（id={}），奖励积分: {}", userId, achievement.getName(), achievementId, reward);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndUnlockAchievements(Long userId, String conditionType, Integer currentValue) {
        if (userId == null || conditionType == null || currentValue == null) {
            log.debug("成就校验跳过（参数为空）: userId={}, type={}, value={}", userId, conditionType, currentValue);
            return;
        }
        log.debug("成就校验开始: userId={}, type={}, value={}", userId, conditionType, currentValue);
        List<Achievement> allAchievements = achievementMapper.selectList(Wrappers.emptyWrapper());
        for (Achievement achievement : allAchievements) {
            if (achievement.getConditionType() == null || achievement.getConditionValue() == null) {
                continue;
            }
            if (!conditionType.equals(achievement.getConditionType())) {
                continue;
            }
            if (currentValue >= achievement.getConditionValue()) {
                log.debug("成就条件满足，尝试解锁: userId={}, code={}, current={}, need={}",
                        userId, achievement.getCode(), currentValue, achievement.getConditionValue());
                grantAchievementIfAbsent(userId, achievement);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshContinuousLearningStreak(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            List<String> dayStrings = learningActivityMapper.selectDistinctActivityDayStrings(userId);
            int streak = computeConsecutiveLearningDays(dayStrings, ACTIVITY_ZONE);
            checkAndUnlockAchievements(userId, "continuous_days", streak);
        } catch (Exception e) {
            log.warn("连续学习成就校验失败 userId={} msg={}", userId, e.getMessage());
        }
    }

    static int computeConsecutiveLearningDays(List<String> isoDates, ZoneId zone) {
        if (isoDates == null || isoDates.isEmpty()) {
            return 0;
        }
        Set<LocalDate> days = new HashSet<>();
        for (String s : isoDates) {
            if (s == null || s.isBlank()) {
                continue;
            }
            try {
                days.add(LocalDate.parse(s.trim()));
            } catch (Exception ignored) {
                // 跳过异常格式
            }
        }
        if (days.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now(zone);
        LocalDate cursor = today;
        if (!days.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    @Override
    public int getUnlockedCount(Long userId) {
        return userAchievementMapper.countByUserId(userId);
    }

    @Override
    public int getTotalAchievementCount() {
        return Math.toIntExact(achievementMapper.selectCount(null));
    }
}
