package com.anti.mapper;

import com.anti.entity.Leaderboard;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 排行榜Mapper接口
 */
@Mapper
public interface LeaderboardMapper extends BaseMapper<Leaderboard> {

    /**
     * 获取日排行榜
     */
    @Select("SELECT l.*, u.nickname, u.avatar, u.grade, u.major FROM leaderboard l " +
            "LEFT JOIN sys_user u ON l.user_id = u.id " +
            "WHERE l.period_type = 'daily' AND l.update_date = CURDATE() " +
            "ORDER BY l.`rank` ASC LIMIT #{limit}")
    List<Leaderboard> selectDailyTop(@Param("limit") int limit);

    /**
     * 获取周排行榜
     */
    @Select("SELECT l.*, u.nickname, u.avatar, u.grade, u.major FROM leaderboard l " +
            "LEFT JOIN sys_user u ON l.user_id = u.id " +
            "WHERE l.period_type = 'weekly' " +
            "ORDER BY l.`rank` ASC LIMIT #{limit}")
    List<Leaderboard> selectWeeklyTop(@Param("limit") int limit);

    /**
     * 获取总排行榜
     */
    @Select("SELECT l.*, u.nickname, u.avatar, u.grade, u.major FROM leaderboard l " +
            "LEFT JOIN sys_user u ON l.user_id = u.id " +
            "WHERE l.period_type = 'all' " +
            "ORDER BY l.`rank` ASC LIMIT #{limit}")
    List<Leaderboard> selectAllTimeTop(@Param("limit") int limit);

    /**
     * 获取用户排名
     */
    @Select("SELECT * FROM leaderboard WHERE user_id = #{userId} AND period_type = #{periodType}")
    Leaderboard selectUserRank(@Param("userId") Long userId, @Param("periodType") String periodType);

    /**
     * 更新用户积分
     */
    @Update("UPDATE leaderboard SET score = score + #{addScore}, `rank` = #{rank} WHERE user_id = #{userId} AND period_type = #{periodType}")
    void addScore(@Param("userId") Long userId, @Param("addScore") int addScore, @Param("rank") int rank, @Param("periodType") String periodType);
}
