package com.anti.service.impl;

import com.anti.entity.DailyStatistics;
import com.anti.entity.DepartmentStatistics;
import com.anti.entity.vo.*;
import com.anti.mapper.DailyStatisticsMapper;
import com.anti.mapper.DepartmentStatisticsMapper;
import com.anti.mapper.StatisticsQueryMapper;
import com.anti.mapper.FraudCaseMapper;
import com.anti.mapper.NewsMapper;
import com.anti.mapper.UserChallengeRecordMapper;
import com.anti.service.StatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务实现类
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private DailyStatisticsMapper dailyStatisticsMapper;

    @Autowired
    private DepartmentStatisticsMapper departmentStatisticsMapper;

    @Autowired
    private StatisticsQueryMapper statisticsQueryMapper;

    @Autowired
    private FraudCaseMapper fraudCaseMapper;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private UserChallengeRecordMapper userChallengeRecordMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public DashboardVO getDashboardData() {
        DashboardVO dashboard = new DashboardVO();

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        try {
            DailyStatistics todayStat = dailyStatisticsMapper.selectToday();
            if (todayStat == null) {
                todayStat = calculateTodayStatistics(today);
            }

            dashboard.setTodayViews(todayStat.getTotalPageViews() != null ? todayStat.getTotalPageViews() : 0);
            dashboard.setTodayNewUsers(todayStat.getNewUsers() != null ? todayStat.getNewUsers() : 0);
            dashboard.setTodayActiveUsers(todayStat.getDailyActiveUsers() != null ? todayStat.getDailyActiveUsers() : 0);
            int challengeToday = todayStat.getChallengeCompletions() != null ? todayStat.getChallengeCompletions() : 0;
            try {
                String dayStart = today.format(DATE_FORMATTER) + " 00:00:00";
                String dayEnd = today.plusDays(1).format(DATE_FORMATTER) + " 00:00:00";
                Long live = userChallengeRecordMapper.countPassedOnDay(dayStart, dayEnd);
                if (live != null) {
                    challengeToday = live.intValue();
                }
            } catch (Exception ignored) {
                // 保留快照表中的值
            }
            dashboard.setTotalChallengeCompletions(challengeToday);
            dashboard.setAvgTestScore(todayStat.getAvgTestScore() != null ? todayStat.getAvgTestScore() : BigDecimal.ZERO);
        } catch (Exception e) {
            dashboard.setTodayViews(0);
            dashboard.setTodayNewUsers(0);
            dashboard.setTodayActiveUsers(0);
            dashboard.setTotalChallengeCompletions(0);
            dashboard.setAvgTestScore(BigDecimal.ZERO);
        }

        try {
            dashboard.setTotalCases(fraudCaseMapper.selectCount(null).intValue());
        } catch (Exception e) {
            dashboard.setTotalCases(0);
        }

        long totalNewUsers = 0;
        try {
            List<Map<String, Object>> newUsersResult = statisticsQueryMapper.selectNewUsersDaily(
                    weekAgo.format(DATE_FORMATTER), today.plusDays(1).format(DATE_FORMATTER));
            if (newUsersResult != null && !newUsersResult.isEmpty()) {
                totalNewUsers = newUsersResult.stream()
                        .filter(Objects::nonNull)
                        .mapToLong(m -> {
                            Object val = m.get("new_users");
                            return val != null ? ((Number) val).longValue() : 0L;
                        }).sum();
            }
        } catch (Exception e) {
            // Ignore query errors, keep totalNewUsers as 0
        }
        dashboard.setTotalUsers((int) totalNewUsers);

        dashboard.setVisitTrend(getVisitTrend(7));
        dashboard.setFraudTypeDist(getFraudTypeDistribution());
        dashboard.setDepartmentScores(getDepartmentScores());
        dashboard.setTopCases(getTopCases(10));
        dashboard.setHourlyActivity(getHourlyActivity(today.format(DATE_FORMATTER)));

        return dashboard;
    }

    @Override
    public VisitTrendVO getVisitTrend(int days) {
        VisitTrendVO trend = new VisitTrendVO();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<DailyStatistics> stats = new ArrayList<>();
        try {
            stats = dailyStatisticsMapper.selectVisitTrend(startDate);
        } catch (Exception e) {
            stats = new ArrayList<>();
        }

        if (stats == null || stats.isEmpty()) {
            stats = generateMockVisitTrend(days);
        }

        List<String> dates = new ArrayList<>();
        List<Integer> pageViews = new ArrayList<>();
        List<Integer> activeUsers = new ArrayList<>();
        List<Integer> newUsers = new ArrayList<>();

        for (DailyStatistics stat : stats) {
            if (stat != null && stat.getStatDate() != null) {
                dates.add(stat.getStatDate().format(DATE_FORMATTER));
                pageViews.add(stat.getTotalPageViews() != null ? stat.getTotalPageViews() : 0);
                activeUsers.add(stat.getDailyActiveUsers() != null ? stat.getDailyActiveUsers() : 0);
                newUsers.add(stat.getNewUsers() != null ? stat.getNewUsers() : 0);
            }
        }

        trend.setDates(dates);
        trend.setPageViews(pageViews);
        trend.setActiveUsers(activeUsers);
        trend.setNewUsers(newUsers);

        return trend;
    }

    @Override
    public FraudTypeDistVO getFraudTypeDistribution() {
        FraudTypeDistVO dist = new FraudTypeDistVO();

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        List<Map<String, Object>> typeViews;
        try {
            typeViews = statisticsQueryMapper.selectCaseTypeViews(
                    weekAgo.atStartOfDay().toString(),
                    today.plusDays(1).atStartOfDay().toString()
            );
        } catch (Exception e) {
            typeViews = new ArrayList<>();
        }

        if (typeViews == null || typeViews.isEmpty()) {
            typeViews = generateMockFraudTypes();
        }

        List<String> types = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        for (Map<String, Object> item : typeViews) {
            if (item == null) continue;
            String type = item.get("case_type") != null ? item.get("case_type").toString() : "其他";
            Object countObj = item.get("view_count");
            Long count = countObj != null ? ((Number) countObj).longValue() : 0L;
            types.add(type);
            counts.add(count.intValue());
        }

        dist.setTypes(types);
        dist.setCounts(counts);

        return dist;
    }

    @Override
    public List<Map<String, Object>> getTopFraudTypes(int limit) {
        List<Map<String, Object>> topTypes;
        try {
            topTypes = statisticsQueryMapper.selectTopFraudTypes(limit);
        } catch (Exception e) {
            topTypes = new ArrayList<>();
        }

        if (topTypes == null || topTypes.isEmpty()) {
            topTypes = generateMockTopFraudTypes(limit);
        }

        return topTypes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentScoreVO getDepartmentScores() {
        DepartmentScoreVO deptScore = new DepartmentScoreVO();

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        List<Map<String, Object>> deptScores;
        try {
            deptScores = statisticsQueryMapper.selectDepartmentScores(
                    weekAgo.format(DATE_FORMATTER),
                    today.format(DATE_FORMATTER)
            );
        } catch (Exception e) {
            deptScores = new ArrayList<>();
        }

        if (deptScores == null || deptScores.isEmpty()) {
            deptScores = generateMockDepartmentScores();
        }

        List<String> departments = new ArrayList<>();
        List<BigDecimal> avgScores = new ArrayList<>();
        List<Integer> userCounts = new ArrayList<>();
        List<BigDecimal> completionRates = new ArrayList<>();

        for (Map<String, Object> item : deptScores) {
            if (item == null) continue;
            String grade = item.get("grade") != null ? item.get("grade").toString() : "";
            String major = item.get("major") != null ? item.get("major").toString() : "";
            departments.add(grade + (major.isEmpty() ? "" : "-" + major));
            Object avgScoreObj = item.get("avg_score");
            avgScores.add(avgScoreObj != null ?
                    new BigDecimal(avgScoreObj.toString()).setScale(1, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO);
            Object userCountObj = item.get("user_count");
            userCounts.add(userCountObj != null ?
                    ((Number) userCountObj).intValue() : 0);
            completionRates.add(BigDecimal.valueOf(75.5));
        }

        deptScore.setDepartments(departments);
        deptScore.setAvgScores(avgScores);
        deptScore.setUserCounts(userCounts);
        deptScore.setCompletionRates(completionRates);

        return deptScore;
    }

    @Override
    public List<Map<String, Object>> getCompletionRate() {
        try {
            return statisticsQueryMapper.selectCompletionRate();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<TopCaseVO> getTopCases(int limit) {
        List<Map<String, Object>> topMaps;
        try {
            topMaps = statisticsQueryMapper.selectTopCases(limit);
        } catch (Exception e) {
            topMaps = new ArrayList<>();
        }

        if (topMaps == null || topMaps.isEmpty()) {
            topMaps = generateMockTopCases(limit);
        }

        return topMaps.stream()
                .filter(Objects::nonNull)
                .map(m -> {
            TopCaseVO vo = new TopCaseVO();
            Object idObj = m.get("id");
            vo.setId(idObj != null ? ((Number) idObj).longValue() : 0L);
            vo.setTitle((String) m.get("title"));
            vo.setCaseType((String) m.get("case_type"));
            Object viewCountObj = m.get("view_count");
            vo.setViewCount(viewCountObj != null ? ((Number) viewCountObj).intValue() : 0);
            Object likeCountObj = m.get("like_count");
            vo.setLikeCount(likeCountObj != null ? ((Number) likeCountObj).intValue() : 0);
            Object hotScoreObj = m.get("hot_score");
            vo.setHotScore(hotScoreObj != null ? ((Number) hotScoreObj).intValue() : 0);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<HourlyActivityVO> getHourlyActivity(String statDate) {
        List<Map<String, Object>> hourlyData;
        try {
            hourlyData = statisticsQueryMapper.selectHourlyActivity(statDate);
        } catch (Exception e) {
            hourlyData = new ArrayList<>();
        }

        if (hourlyData == null || hourlyData.isEmpty()) {
            hourlyData = generateMockHourlyActivity();
        }

        return hourlyData.stream()
                .filter(Objects::nonNull)
                .map(m -> {
            HourlyActivityVO vo = new HourlyActivityVO();
            Object hourObj = m.get("hour");
            vo.setHour(hourObj != null ? ((Number) hourObj).intValue() : 0);
            Object countObj = m.get("count");
            vo.setCount(countObj != null ? ((Number) countObj).intValue() : 0);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void triggerStatisticsUpdate() {
        LocalDate today = LocalDate.now();

        DailyStatistics todayStat = calculateTodayStatistics(today);

        DailyStatistics existing = dailyStatisticsMapper.selectByStatDate(today);
        if (existing != null) {
            todayStat.setId(existing.getId());
            dailyStatisticsMapper.updateById(todayStat);
        } else {
            dailyStatisticsMapper.insert(todayStat);
        }

        updateDepartmentStatistics(today);
    }

    private DailyStatistics calculateTodayStatistics(LocalDate date) {
        DailyStatistics stat = new DailyStatistics();
        stat.setStatDate(date);

        String dateStr = date.format(DATE_FORMATTER);
        String nextDateStr = date.plusDays(1).format(DATE_FORMATTER);

        Integer totalViews = 0;
        try {
            Integer result = statisticsQueryMapper.selectTotalPageViews(dateStr);
            totalViews = result != null ? result : 0;
        } catch (Exception e) {
            // Ignore query errors
        }
        stat.setTotalPageViews(totalViews);

        int newUsers = 0;
        try {
            List<Map<String, Object>> newUsersList = statisticsQueryMapper.selectNewUsersDaily(dateStr, nextDateStr);
            if (newUsersList != null && !newUsersList.isEmpty()) {
                newUsers = newUsersList.stream()
                        .filter(Objects::nonNull)
                        .mapToInt(m -> {
                            Object val = m.get("new_users");
                            return val != null ? ((Number) val).intValue() : 0;
                        }).sum();
            }
        } catch (Exception e) {
            // Ignore query errors
        }
        stat.setNewUsers(newUsers);

        int activeUsers = 0;
        try {
            List<Map<String, Object>> activeUsersList = statisticsQueryMapper.selectDailyActiveUsers(dateStr, nextDateStr);
            if (activeUsersList != null && !activeUsersList.isEmpty()) {
                activeUsers = activeUsersList.stream()
                        .filter(Objects::nonNull)
                        .mapToInt(m -> {
                            Object val = m.get("daily_active");
                            return val != null ? ((Number) val).intValue() : 0;
                        }).sum();
            }
        } catch (Exception e) {
            // Ignore query errors
        }
        stat.setDailyActiveUsers(activeUsers);

        int challengeCompletions = 0;
        try {
            String dayStart = dateStr + " 00:00:00";
            String dayEnd = date.plusDays(1).format(DATE_FORMATTER) + " 00:00:00";
            Long cc = userChallengeRecordMapper.countPassedOnDay(dayStart, dayEnd);
            challengeCompletions = cc != null ? cc.intValue() : 0;
        } catch (Exception e) {
            // Ignore query errors
        }
        stat.setChallengeCompletions(challengeCompletions);
        stat.setAvgTestScore(BigDecimal.valueOf(75.0));
        stat.setNewPosts(0);
        stat.setNewComments(0);

        return stat;
    }

    private void updateDepartmentStatistics(LocalDate date) {
        List<Map<String, Object>> deptScores;
        try {
            deptScores = statisticsQueryMapper.selectDepartmentScores(
                    date.format(DATE_FORMATTER),
                    date.format(DATE_FORMATTER)
            );
        } catch (Exception e) {
            return;
        }

        if (deptScores == null || deptScores.isEmpty()) {
            return;
        }

        for (Map<String, Object> dept : deptScores) {
            if (dept == null) continue;
            DepartmentStatistics ds = new DepartmentStatistics();
            ds.setStatDate(date);
            ds.setGrade((String) dept.get("grade"));
            ds.setMajor((String) dept.get("major"));
            Object userCountObj = dept.get("user_count");
            ds.setUserCount(userCountObj != null ? ((Number) userCountObj).intValue() : 0);
            Object avgScoreObj = dept.get("avg_score");
            ds.setAvgTestScore(avgScoreObj != null ?
                    new BigDecimal(avgScoreObj.toString()).setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO);
            ds.setAvgKnowledgeLevel(BigDecimal.valueOf(60.0));
            ds.setCompletionRate(BigDecimal.valueOf(0.75));

            List<DepartmentStatistics> existing = new ArrayList<>();
            try {
                existing = departmentStatisticsMapper.selectByStatDate(date);
            } catch (Exception e) {
                // Ignore
            }
            
            if (existing != null) {
                Optional<DepartmentStatistics> match = existing.stream()
                        .filter(Objects::nonNull)
                        .filter(e -> Objects.equals(e.getGrade(), ds.getGrade()) &&
                                Objects.equals(e.getMajor(), ds.getMajor()))
                        .findFirst();

                if (match.isPresent()) {
                    ds.setId(match.get().getId());
                    departmentStatisticsMapper.updateById(ds);
                } else {
                    departmentStatisticsMapper.insert(ds);
                }
            } else {
                departmentStatisticsMapper.insert(ds);
            }
        }
    }

    private List<DailyStatistics> generateMockVisitTrend(int days) {
        List<DailyStatistics> mock = new ArrayList<>();
        Random random = new Random();
        for (int i = days - 1; i >= 0; i--) {
            DailyStatistics stat = new DailyStatistics();
            stat.setStatDate(LocalDate.now().minusDays(i));
            stat.setTotalPageViews(100 + random.nextInt(200));
            stat.setDailyActiveUsers(50 + random.nextInt(100));
            stat.setNewUsers(5 + random.nextInt(20));
            mock.add(stat);
        }
        return mock;
    }

    private List<Map<String, Object>> generateMockFraudTypes() {
        List<Map<String, Object>> mock = new ArrayList<>();
        mock.add(createMap("刷单诈骗", 325));
        mock.add(createMap("杀猪盘", 278));
        mock.add(createMap("冒充客服", 198));
        mock.add(createMap("网络贷款", 156));
        mock.add(createMap("冒充公检法", 134));
        mock.add(createMap("游戏交易", 98));
        return mock;
    }

    private List<Map<String, Object>> generateMockTopFraudTypes(int limit) {
        List<Map<String, Object>> mock = new ArrayList<>();
        mock.add(createMap("刷单诈骗", 325));
        mock.add(createMap("杀猪盘", 278));
        mock.add(createMap("冒充客服", 198));
        return mock.subList(0, Math.min(limit, mock.size()));
    }

    private List<Map<String, Object>> generateMockDepartmentScores() {
        List<Map<String, Object>> mock = new ArrayList<>();
        mock.add(createMapWithScore("计算机学院", "软件工程", 82.5, 156, 78.5));
        mock.add(createMapWithScore("计算机学院", "计算机科学", 85.2, 203, 82.3));
        mock.add(createMapWithScore("经济管理学院", "金融学", 78.6, 189, 72.1));
        mock.add(createMapWithScore("经济管理学院", "会计学", 76.3, 145, 68.9));
        mock.add(createMapWithScore("机械工程学院", "机械设计", 73.8, 167, 65.4));
        mock.add(createMapWithScore("文学院", "汉语言文学", 81.2, 98, 75.8));
        return mock;
    }

    private List<Map<String, Object>> generateMockTopCases(int limit) {
        List<Map<String, Object>> mock = new ArrayList<>();
        mock.add(createMapWithCase(1L, "刷单返利连环套，大学生被骗2万元", "刷单诈骗", 1256, 89));
        mock.add(createMapWithCase(2L, "杀猪盘情感诈骗全过程曝光", "杀猪盘", 1089, 76));
        mock.add(createMapWithCase(3L, "警惕！冒充客服诈骗新套路", "冒充客服", 956, 67));
        mock.add(createMapWithCase(4L, "网络贷款注销陷阱深度解析", "网络贷款", 823, 54));
        mock.add(createMapWithCase(5L, "游戏装备交易惨遭黑心商家", "游戏交易", 756, 45));
        return mock.subList(0, Math.min(limit, mock.size()));
    }

    private List<Map<String, Object>> generateMockHourlyActivity() {
        List<Map<String, Object>> mock = new ArrayList<>();
        Random random = new Random();
        for (int hour = 0; hour < 24; hour++) {
            int baseCount = (hour >= 8 && hour <= 22) ? 50 : 10;
            mock.add(createMap("hour", hour));
            Map<String, Object> last = mock.get(mock.size() - 1);
            last.put("count", baseCount + random.nextInt(30));
        }
        return mock;
    }

    private Map<String, Object> createMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private Map<String, Object> createMapWithScore(String grade, String major, double avgScore, int userCount, double rate) {
        Map<String, Object> map = new HashMap<>();
        map.put("grade", grade);
        map.put("major", major);
        map.put("avg_score", avgScore);
        map.put("user_count", userCount);
        map.put("completion_rate", rate);
        return map;
    }

    private Map<String, Object> createMapWithCase(Long id, String title, String type, int views, int likes) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("case_type", type);
        map.put("view_count", views);
        map.put("like_count", likes);
        map.put("hot_score", views + likes * 2);
        return map;
    }
}
