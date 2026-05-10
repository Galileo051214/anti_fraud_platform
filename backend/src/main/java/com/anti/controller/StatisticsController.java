package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.vo.*;
import com.anti.mapper.DailyStatisticsMapper;
import com.anti.mapper.DepartmentStatisticsMapper;
import com.anti.service.StatisticsService;
import com.anti.entity.DailyStatistics;
import com.anti.entity.DepartmentStatistics;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 数据统计控制器
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private DailyStatisticsMapper dailyStatisticsMapper;

    @Autowired
    private DepartmentStatisticsMapper departmentStatisticsMapper;

    /**
     * 获取管理后台看板数据
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DashboardVO> getDashboard() {
        return Result.success(statisticsService.getDashboardData());
    }

    /**
     * 获取访问量趋势
     */
    @GetMapping("/visit/trend")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<VisitTrendVO> getVisitTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(statisticsService.getVisitTrend(days));
    }

    /**
     * 获取诈骗类型分布
     */
    @GetMapping("/fraud/types")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<FraudTypeDistVO> getFraudTypeDistribution() {
        return Result.success(statisticsService.getFraudTypeDistribution());
    }

    /**
     * 获取高频诈骗类型TOP N
     */
    @GetMapping("/fraud/top")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getTopFraudTypes(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(statisticsService.getTopFraudTypes(limit));
    }

    /**
     * 获取各院系测试得分统计
     */
    @GetMapping("/department/scores")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DepartmentScoreVO> getDepartmentScores() {
        return Result.success(statisticsService.getDepartmentScores());
    }

    /**
     * 获取学生学习完成率统计
     */
    @GetMapping("/completion/rate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getCompletionRate() {
        return Result.success(statisticsService.getCompletionRate());
    }

    /**
     * 获取TOP案例排行榜
     */
    @GetMapping("/cases/top")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<TopCaseVO>> getTopCases(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(statisticsService.getTopCases(limit));
    }

    /**
     * 获取用户活跃度热力图数据
     */
    @GetMapping("/activity/hourly")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<HourlyActivityVO>> getHourlyActivity(
            @RequestParam(required = false) String statDate) {
        if (statDate == null || statDate.isEmpty()) {
            statDate = java.time.LocalDate.now().toString();
        }
        return Result.success(statisticsService.getHourlyActivity(statDate));
    }

    /**
     * 手动触发统计数据更新
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> refreshStatistics() {
        statisticsService.triggerStatisticsUpdate();
        return Result.success();
    }

    /**
     * 导出每日统计数据(Excel)
     */
    @GetMapping("/export/daily")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportDailyStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {
        List<DailyStatistics> list;
        if (startDate != null && endDate != null) {
            list = dailyStatisticsMapper.selectRecentDays(LocalDate.parse(startDate));
        } else {
            list = dailyStatisticsMapper.selectRecentDays(LocalDate.now().minusMonths(1));
        }

        List<StatisticsExportVO> exportList = list.stream().map(stat -> {
            StatisticsExportVO vo = new StatisticsExportVO();
            vo.setStatDate(stat.getStatDate());
            vo.setDailyActiveUsers(stat.getDailyActiveUsers());
            vo.setNewUsers(stat.getNewUsers());
            vo.setTotalPageViews(stat.getTotalPageViews());
            vo.setChallengeCompletions(stat.getChallengeCompletions());
            vo.setAvgTestScore(stat.getAvgTestScore());
            vo.setNewPosts(stat.getNewPosts());
            vo.setNewComments(stat.getNewComments());
            vo.setCreateTime(stat.getCreateTime() != null ? stat.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            return vo;
        }).collect(java.util.stream.Collectors.toList());

        String filename = URLEncoder.encode("每日统计数据_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + filename + ".xlsx");
        EasyExcel.write(response.getOutputStream(), StatisticsExportVO.class).sheet("每日统计").doWrite(exportList);
    }

    /**
     * 导出院系统计数据(Excel)
     */
    @GetMapping("/export/department")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportDepartmentStatistics(
            @RequestParam(required = false) String statDate,
            HttpServletResponse response) throws IOException {
        List<DepartmentStatistics> list;
        if (statDate != null) {
            list = departmentStatisticsMapper.selectByStatDate(LocalDate.parse(statDate));
        } else {
            list = departmentStatisticsMapper.selectLatest();
        }

        List<DepartmentExportVO> exportList = list.stream().map(stat -> {
            DepartmentExportVO vo = new DepartmentExportVO();
            vo.setStatDate(stat.getStatDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            vo.setGrade(stat.getGrade());
            vo.setMajor(stat.getMajor());
            vo.setUserCount(stat.getUserCount());
            vo.setAvgKnowledgeLevel(stat.getAvgKnowledgeLevel());
            vo.setAvgTestScore(stat.getAvgTestScore());
            vo.setCompletionRate(stat.getCompletionRate());
            return vo;
        }).collect(java.util.stream.Collectors.toList());

        String filename = URLEncoder.encode("院系统计数据_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + filename + ".xlsx");
        EasyExcel.write(response.getOutputStream(), DepartmentExportVO.class).sheet("院系统计").doWrite(exportList);
    }
}
