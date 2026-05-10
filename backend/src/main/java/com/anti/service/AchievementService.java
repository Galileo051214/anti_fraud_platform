package com.anti.service;

import com.anti.entity.Achievement;
import com.anti.entity.UserAchievement;

import java.util.List;

public interface AchievementService {

    List<Achievement> getAllAchievements();

    List<UserAchievement> getUserAchievements(Long userId);

    boolean unlockAchievement(Long userId, String achievementCode);

    void checkAndUnlockAchievements(Long userId, String conditionType, Integer currentValue);

    /**
     * 根据多源学习行为聚合连续学习天数，校验 continuous_days 类成就
     */
    void refreshContinuousLearningStreak(Long userId);

    int getUnlockedCount(Long userId);

    int getTotalAchievementCount();
}
