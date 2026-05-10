package com.anti.mapper;

import com.anti.entity.DailyStatistics;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日统计Mapper接口
 */
@Mapper
public interface DailyStatisticsMapper extends BaseMapper<DailyStatistics> {

    /**
     * 根据日期查询统计数据
     */
    @Select("SELECT * FROM daily_statistics WHERE stat_date = #{statDate}")
    DailyStatistics selectByStatDate(@Param("statDate") LocalDate statDate);

    /**
     * 查询最近N天的统计数据
     */
    @Select("SELECT * FROM daily_statistics WHERE stat_date >= #{startDate} ORDER BY stat_date ASC")
    List<DailyStatistics> selectRecentDays(@Param("startDate") LocalDate startDate);

    /**
     * 查询最近N天的访问量趋势
     */
    @Select("SELECT stat_date, total_page_views, daily_active_users, new_users FROM daily_statistics " +
            "WHERE stat_date >= #{startDate} ORDER BY stat_date ASC")
    List<DailyStatistics> selectVisitTrend(@Param("startDate") LocalDate startDate);

    /**
     * 查询最近N天的案例浏览趋势
     */
    @Select("SELECT stat_date, case_views FROM daily_statistics " +
            "WHERE stat_date >= #{startDate} ORDER BY stat_date ASC")
    List<DailyStatistics> selectCaseViewTrend(@Param("startDate") LocalDate startDate);

    /**
     * 获取今日统计数据
     */
    @Select("SELECT * FROM daily_statistics WHERE stat_date = CURDATE()")
    DailyStatistics selectToday();
}
