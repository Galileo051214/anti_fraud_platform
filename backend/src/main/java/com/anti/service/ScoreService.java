package com.anti.service;

import com.anti.entity.UserScore;
import com.anti.entity.dto.ScoreChangeRequest;

public interface ScoreService {

    UserScore getScoreByUserId(Long userId);

    void addScore(Long userId, Integer score, String reason);

    void deductScore(Long userId, Integer score, String reason);

    int calculateLevel(Integer totalScore);

    void initScore(Long userId);

    void resetWeeklyScore(Long userId);
}
