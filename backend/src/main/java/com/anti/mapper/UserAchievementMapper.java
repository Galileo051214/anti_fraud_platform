package com.anti.mapper;

import com.anti.entity.UserAchievement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAchievementMapper extends BaseMapper<UserAchievement> {

    List<UserAchievement> selectByUserId(@Param("userId") Long userId);

    /** 使用 COUNT，避免 EXISTS 映射到 boolean 在部分驱动下不稳定 */
    @Select("SELECT COUNT(*) FROM user_achievement WHERE user_id = #{userId} AND achievement_id = #{achievementId}")
    int countUserAchievementLink(@Param("userId") Long userId, @Param("achievementId") Long achievementId);

    /**
     * 首次写入成功返回 1；已存在（唯一键冲突）返回 0。用于与积分发放原子串行、避免并发重复加分。
     */
    @Insert("INSERT IGNORE INTO user_achievement (user_id, achievement_id) VALUES (#{userId}, #{achievementId})")
    Integer insertIfAbsent(@Param("userId") Long userId, @Param("achievementId") Long achievementId);

    int countByUserId(@Param("userId") Long userId);
}
