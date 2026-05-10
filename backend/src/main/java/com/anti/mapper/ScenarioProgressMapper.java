package com.anti.mapper;

import com.anti.entity.ScenarioProgress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 情景模拟进度Mapper接口
 */
@Mapper
public interface ScenarioProgressMapper extends BaseMapper<ScenarioProgress> {

    /**
     * 查询用户特定情景模拟的进度
     */
    @Select("SELECT * FROM scenario_progress WHERE user_id = #{userId} AND challenge_id = #{challengeId}")
    ScenarioProgress selectByUserAndChallenge(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    /**
     * 查询用户进行中的情景模拟
     */
    @Select("SELECT * FROM scenario_progress WHERE user_id = #{userId} AND status = 'in_progress'")
    ScenarioProgress selectInProgress(@Param("userId") Long userId);

    /**
     * 查询用户完成的情景模拟数量
     */
    @Select("SELECT COUNT(*) FROM scenario_progress WHERE user_id = #{userId} AND status IN ('completed', 'failed')")
    int countCompletedScenarios(@Param("userId") Long userId);
}
