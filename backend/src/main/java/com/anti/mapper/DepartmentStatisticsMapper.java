package com.anti.mapper;

import com.anti.entity.DepartmentStatistics;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 院系统计Mapper接口
 */
@Mapper
public interface DepartmentStatisticsMapper extends BaseMapper<DepartmentStatistics> {

    /**
     * 根据日期查询院系统计数据
     */
    @Select("SELECT * FROM department_statistics WHERE stat_date = #{statDate}")
    List<DepartmentStatistics> selectByStatDate(@Param("statDate") LocalDate statDate);

    /**
     * 查询最近N天的院系统计
     */
    @Select("SELECT * FROM department_statistics WHERE stat_date >= #{startDate} ORDER BY stat_date ASC, user_count DESC")
    List<DepartmentStatistics> selectRecentDays(@Param("startDate") LocalDate startDate);

    /**
     * 按年级统计
     */
    @Select("SELECT grade, SUM(user_count) as user_count, AVG(avg_test_score) as avg_test_score, AVG(completion_rate) as completion_rate " +
            "FROM department_statistics WHERE stat_date = #{statDate} GROUP BY grade ORDER BY user_count DESC")
    List<DepartmentStatistics> selectByGrade(@Param("statDate") LocalDate statDate);

    /**
     * 获取最新的院系统计
     */
    @Select("SELECT * FROM department_statistics WHERE stat_date = (SELECT MAX(stat_date) FROM department_statistics)")
    List<DepartmentStatistics> selectLatest();
}
