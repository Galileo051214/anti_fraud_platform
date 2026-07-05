package com.anti.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Agent模拟挑战每日奖励记录
 */
@Data
@TableName("agent_challenge_daily_reward")
public class AgentChallengeDailyReward {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate rewardDate;

    private String sessionId;

    private Long challengeId;

    private Integer score;

    private Integer rewardScore;

    private LocalDateTime createTime;
}
