package com.anti.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户学习活跃日（用于「连续学习」成就）
 */
@Mapper
public interface LearningActivityMapper {

    @Select("""
            SELECT DISTINCT DATE_FORMAT(activity_day, '%Y-%m-%d') FROM (
                SELECT DATE(browse_time) AS activity_day FROM case_browse_log WHERE user_id = #{userId}
                UNION
                SELECT DATE(browse_time) FROM news_browse_log WHERE user_id = #{userId}
                UNION
                SELECT DATE(end_time) FROM user_challenge_record WHERE user_id = #{userId}
                UNION
                SELECT DATE(create_time) FROM forum_post WHERE user_id = #{userId} AND status = 1
                UNION
                SELECT DATE(update_time) FROM scenario_progress WHERE user_id = #{userId} AND status IN ('completed', 'failed')
                UNION
                SELECT DATE(last_login_time) FROM sys_user WHERE id = #{userId} AND last_login_time IS NOT NULL
            ) t
            ORDER BY 1 DESC
            """)
    List<String> selectDistinctActivityDayStrings(@Param("userId") Long userId);
}
