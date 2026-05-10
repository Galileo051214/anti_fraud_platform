package com.anti.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 统计查询Mapper接口 - 用于复杂统计查询
 */
@Mapper
public interface StatisticsQueryMapper {

    /**
     * 获取各类型案例浏览量统计
     */
    @Select("SELECT fc.case_type, COUNT(cbl.id) as view_count " +
            "FROM case_browse_log cbl " +
            "INNER JOIN fraud_case fc ON cbl.case_id = fc.id " +
            "WHERE cbl.browse_time >= #{startTime} AND cbl.browse_time < #{endTime} " +
            "GROUP BY fc.case_type")
    List<Map<String, Object>> selectCaseTypeViews(@Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 获取高频诈骗类型统计(TOP N)
     */
    @Select("SELECT ct.name as fraud_type, COUNT(*) as count " +
            "FROM case_browse_log cbl " +
            "INNER JOIN case_tag_relation ctr ON cbl.case_id = ctr.case_id " +
            "INNER JOIN case_tag ct ON ctr.tag_id = ct.id " +
            "GROUP BY ct.name " +
            "ORDER BY count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectTopFraudTypes(@Param("limit") int limit);

    /**
     * 获取新增用户数统计
     */
    @Select("SELECT DATE(create_time) as stat_date, COUNT(*) as new_users " +
            "FROM sys_user " +
            "WHERE create_time >= #{startDate} AND create_time < #{endDate} " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY stat_date ASC")
    List<Map<String, Object>> selectNewUsersDaily(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取日活用户数统计
     */
    @Select("SELECT DATE(browse_time) as stat_date, COUNT(DISTINCT user_id) as daily_active " +
            "FROM case_browse_log " +
            "WHERE browse_time >= #{startDate} AND browse_time < #{endDate} " +
            "GROUP BY DATE(browse_time) " +
            "ORDER BY stat_date ASC")
    List<Map<String, Object>> selectDailyActiveUsers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取总页面浏览量
     */
    @Select("SELECT " +
            "(SELECT COUNT(*) FROM case_browse_log WHERE DATE(browse_time) = #{statDate}) + " +
            "(SELECT COUNT(*) FROM news_browse_log WHERE DATE(browse_time) = #{statDate}) as total_views")
    Integer selectTotalPageViews(@Param("statDate") String statDate);

    /**
     * 获取各院系测试得分统计
     */
    @Select("SELECT u.grade, u.major, AVG(ucr.score) as avg_score, COUNT(DISTINCT u.id) as user_count " +
            "FROM user_challenge_record ucr " +
            "INNER JOIN sys_user u ON ucr.user_id = u.id " +
            "WHERE ucr.end_time >= #{startDate} AND ucr.end_time < #{endDate} " +
            "GROUP BY u.grade, u.major " +
            "ORDER BY avg_score DESC")
    List<Map<String, Object>> selectDepartmentScores(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 获取学生学习完成率
     */
    @Select("SELECT u.grade, u.major, " +
            "COUNT(DISTINCT CASE WHEN ucr.passed = 1 THEN ucr.user_id END) as completed_users, " +
            "COUNT(DISTINCT ucr.user_id) as total_users, " +
            "COUNT(DISTINCT CASE WHEN ucr.passed = 1 THEN ucr.user_id END) * 100.0 / COUNT(DISTINCT ucr.user_id) as completion_rate " +
            "FROM sys_user u " +
            "LEFT JOIN user_challenge_record ucr ON u.id = ucr.user_id " +
            "GROUP BY u.grade, u.major")
    List<Map<String, Object>> selectCompletionRate();

    /**
     * 获取TOP案例排行榜
     */
    @Select("SELECT fc.id, fc.title, fc.view_count, fc.like_count, fc.case_type, " +
            "(fc.view_count + fc.like_count * 2) as hot_score " +
            "FROM fraud_case fc " +
            "WHERE fc.status = 1 " +
            "ORDER BY hot_score DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectTopCases(@Param("limit") int limit);

    /**
     * 获取用户活跃度数据(按小时)
     */
    @Select("SELECT HOUR(browse_time) as hour, COUNT(*) as count " +
            "FROM case_browse_log " +
            "WHERE DATE(browse_time) = #{statDate} " +
            "GROUP BY HOUR(browse_time) " +
            "ORDER BY hour")
    List<Map<String, Object>> selectHourlyActivity(@Param("statDate") String statDate);
}
