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
public interface UserChallengeRecordMapper extends BaseMapper<UserChallengeRecord> {

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

    /**
     * 查询关卡总参与次数
     */
    @Select("SELECT COUNT(*) FROM user_challenge_record WHERE challenge_id = #{challengeId}")
    Long countByChallengeId(@Param("challengeId") Long challengeId);

    /**
     * 查询关卡通过人数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM user_challenge_record WHERE challenge_id = #{challengeId} AND passed = 1")
    Long countPassedByChallengeId(@Param("challengeId") Long challengeId);

    /**
     * 查询所有关卡今日通关数
     */
    @Select("SELECT COUNT(DISTINCT CONCAT(user_id, '-', challenge_id)) FROM user_challenge_record WHERE DATE(end_time) = CURDATE() AND passed = 1")
    Long countTodayPassed();

    /**
     * 指定自然日内，达成通关的「用户-关卡」去重数量（同日同一用户同一关卡多次通关只计 1）
     */
    @Select("SELECT COUNT(DISTINCT CONCAT(user_id, '-', challenge_id)) FROM user_challenge_record " +
            "WHERE passed = 1 AND end_time >= #{dayStart} AND end_time < #{dayEnd}")
    Long countPassedOnDay(@Param("dayStart") String dayStart, @Param("dayEnd") String dayEnd);

    /**
     * 查询总通关人数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM user_challenge_record WHERE passed = 1")
    Long countTotalPassedUsers();

    /**
     * 查询所有参与次数
     */
    @Select("SELECT COUNT(*) FROM user_challenge_record")
    Long countAllAttempts();
}
