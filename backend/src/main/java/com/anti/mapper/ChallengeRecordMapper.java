package com.anti.mapper;

import com.anti.entity.UserChallengeRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户闯关记录Mapper接口
 */
@Mapper
public interface ChallengeRecordMapper extends BaseMapper<UserChallengeRecord> {

    /**
     * 查询用户所有闯关记录
     */
    @Select("SELECT * FROM user_challenge_record WHERE user_id = #{userId} ORDER BY end_time DESC")
    List<UserChallengeRecord> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询用户特定关卡的最新记录
     */
    @Select("SELECT * FROM user_challenge_record WHERE user_id = #{userId} AND challenge_id = #{challengeId} ORDER BY end_time DESC LIMIT 1")
    UserChallengeRecord selectLatestByUserAndChallenge(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    /**
     * 查询用户已通关的关卡ID列表
     */
    @Select("SELECT challenge_id FROM user_challenge_record WHERE user_id = #{userId} AND passed = 1")
    List<Long> selectPassedChallengeIds(@Param("userId") Long userId);

    /**
     * 查询用户通关数量
     */
    @Select("SELECT COUNT(DISTINCT challenge_id) FROM user_challenge_record WHERE user_id = #{userId} AND passed = 1")
    int countPassedChallenges(@Param("userId") Long userId);

    /**
     * 查询用户历史最高分
     */
    @Select("SELECT MAX(score) FROM user_challenge_record WHERE user_id = #{userId} AND challenge_id = #{challengeId}")
    Integer selectHighestScore(@Param("userId") Long userId, @Param("challengeId") Long challengeId);
}
