package com.anti.mapper;

import com.anti.entity.AgentChallengeDailyReward;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * Agent模拟挑战每日奖励Mapper
 */
@Mapper
public interface AgentChallengeDailyRewardMapper extends BaseMapper<AgentChallengeDailyReward> {

    @Select("SELECT COUNT(*) FROM agent_challenge_daily_reward WHERE user_id = #{userId} AND reward_date = #{rewardDate}")
    int countByUserAndDate(@Param("userId") Long userId, @Param("rewardDate") LocalDate rewardDate);
}
