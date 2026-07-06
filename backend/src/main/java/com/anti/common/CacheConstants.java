package com.anti.common;

/**
 * Redis缓存Key常量类
 * 统一管理所有缓存Key的命名规范和过期时间
 */
public class CacheConstants {

    private CacheConstants() {}

    // ==================== 缓存Key前缀 ====================
    
    /** 热点资讯缓存前缀 */
    public static final String HOT_NEWS_PREFIX = "cache:hot_news:";
    
    /** 热点案例缓存前缀 */
    public static final String HOT_CASE_PREFIX = "cache:hot_case:";
    
    /** 紧急预警缓存前缀 */
    public static final String EMERGENCY_ALERT_PREFIX = "cache:emergency_alert:";
    
    /** 用户会话缓存前缀 */
    public static final String USER_SESSION_PREFIX = "cache:user_session:";
    
    /** 推荐结果缓存前缀 */
    public static final String RECOMMEND_PREFIX = "cache:recommend:";
    
    /** 用户兴趣分析缓存前缀 */
    public static final String USER_INTEREST_PREFIX = "cache:user_interest:";
    
    /** 排行榜缓存前缀 */
    public static final String LEADERBOARD_PREFIX = "cache:leaderboard:";
    
    /** 资讯详情缓存前缀 */
    public static final String NEWS_DETAIL_PREFIX = "cache:news:detail:";
    
    /** 案例详情缓存前缀 */
    public static final String CASE_DETAIL_PREFIX = "cache:case:detail:";
    
    /** 分类列表缓存前缀 */
    public static final String CATEGORY_LIST_PREFIX = "cache:category:list:";
    
    /** 标签列表缓存前缀 */
    public static final String TAG_LIST_PREFIX = "cache:tag:list:";
    
    /** 统计数据缓存前缀 */
    public static final String STATISTICS_PREFIX = "cache:statistics:";
    
    /** 用户画像缓存前缀 */
    public static final String USER_PROFILE_PREFIX = "cache:user_profile:";
    
    /** 验证码缓存前缀 */
    public static final String CAPTCHA_PREFIX = "captcha:";
    
    /** JWT黑名单缓存前缀 */
    public static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";

    // ==================== 缓存过期时间(秒) ====================
    
    /** 热点资讯缓存时间: 5分钟 */
    public static final long HOT_NEWS_CACHE_TTL = 300;
    
    /** 热点案例缓存时间: 5分钟 */
    public static final long HOT_CASE_CACHE_TTL = 300;
    
    /** 紧急预警缓存时间: 1分钟(实时性要求高) */
    public static final long EMERGENCY_ALERT_CACHE_TTL = 60;
    
    /** 用户会话缓存时间: 30分钟 */
    public static final long USER_SESSION_CACHE_TTL = 1800;
    
    /** 推荐结果缓存时间: 1小时 */
    public static final long RECOMMEND_CACHE_TTL = 3600;
    
    /** 用户兴趣分析缓存时间: 2小时 */
    public static final long USER_INTEREST_CACHE_TTL = 7200;
    
    /** 日排行榜缓存时间: 5分钟 */
    public static final long DAILY_LEADERBOARD_CACHE_TTL = 300;
    
    /** 周排行榜缓存时间: 15分钟 */
    public static final long WEEKLY_LEADERBOARD_CACHE_TTL = 900;
    
    /** 总排行榜缓存时间: 30分钟 */
    public static final long ALLTIME_LEADERBOARD_CACHE_TTL = 1800;
    
    /** 资讯详情缓存时间: 10分钟 */
    public static final long NEWS_DETAIL_CACHE_TTL = 600;
    
    /** 案例详情缓存时间: 10分钟 */
    public static final long CASE_DETAIL_CACHE_TTL = 600;
    
    /** 分类列表缓存时间: 1小时 */
    public static final long CATEGORY_LIST_CACHE_TTL = 3600;
    
    /** 标签列表缓存时间: 1小时 */
    public static final long TAG_LIST_CACHE_TTL = 3600;
    
    /** 统计数据缓存时间: 5分钟 */
    public static final long STATISTICS_CACHE_TTL = 300;
    
    /** 用户画像缓存时间: 30分钟 */
    public static final long USER_PROFILE_CACHE_TTL = 1800;
    
    /** 验证码缓存时间: 5分钟 */
    public static final long CAPTCHA_CACHE_TTL = 300;
    
    /** JWT黑名单缓存时间: 与Token有效期一致 */
    public static final long JWT_BLACKLIST_CACHE_TTL = 86400;

    // ==================== 缓存Key组合方法 ====================
    
    /**
     * 获取热点资讯缓存Key
     */
    public static String getHotNewsKey(int limit) {
        return HOT_NEWS_PREFIX + "top:" + limit;
    }
    
    /**
     * 获取热点案例缓存Key
     */
    public static String getHotCaseKey(int limit) {
        return HOT_CASE_PREFIX + "top:" + limit;
    }
    
    /**
     * 获取紧急预警缓存Key
     */
    public static String getEmergencyAlertKey(String alertType) {
        return EMERGENCY_ALERT_PREFIX + alertType;
    }
    
    /**
     * 获取用户会话缓存Key
     */
    public static String getUserSessionKey(Long userId) {
        return USER_SESSION_PREFIX + userId;
    }
    
    /**
     * 获取推荐结果缓存Key
     */
    public static String getRecommendKey(Long userId, int limit) {
        return RECOMMEND_PREFIX + userId + ":limit:" + limit;
    }

    /**
     * 获取按推荐类型区分的推荐结果缓存Key
     */
    public static String getRecommendKey(Long userId, int limit, String itemType) {
        String normalizedType = itemType == null || itemType.isBlank() ? "all" : itemType.trim();
        return RECOMMEND_PREFIX + userId + ":type:" + normalizedType + ":limit:" + limit;
    }
    
    /**
     * 获取用户兴趣分析缓存Key
     */
    public static String getUserInterestKey(Long userId) {
        return USER_INTEREST_PREFIX + userId;
    }
    
    /**
     * 获取日排行榜缓存Key
     */
    public static String getDailyLeaderboardKey(int limit) {
        return LEADERBOARD_PREFIX + "daily:top:" + limit;
    }
    
    /**
     * 获取周排行榜缓存Key
     */
    public static String getWeeklyLeaderboardKey(int limit) {
        return LEADERBOARD_PREFIX + "weekly:top:" + limit;
    }
    
    /**
     * 获取总排行榜缓存Key
     */
    public static String getAllTimeLeaderboardKey(int limit) {
        return LEADERBOARD_PREFIX + "all:top:" + limit;
    }
    
    /**
     * 获取用户排名缓存Key
     */
    public static String getUserRankKey(Long userId, String periodType) {
        return LEADERBOARD_PREFIX + "user:" + userId + ":" + periodType;
    }
    
    /**
     * 获取资讯详情缓存Key
     */
    public static String getNewsDetailKey(Long newsId) {
        return NEWS_DETAIL_PREFIX + newsId;
    }
    
    /**
     * 获取案例详情缓存Key
     */
    public static String getCaseDetailKey(Long caseId) {
        return CASE_DETAIL_PREFIX + caseId;
    }
    
    /**
     * 获取分类列表缓存Key
     */
    public static String getCategoryListKey(String type) {
        return CATEGORY_LIST_PREFIX + (type != null ? type : "all");
    }
    
    /**
     * 获取标签列表缓存Key
     */
    public static String getTagListKey() {
        return TAG_LIST_PREFIX + "all";
    }
    
    /**
     * 获取统计数据缓存Key
     */
    public static String getStatisticsKey(String type) {
        return STATISTICS_PREFIX + type;
    }
    
    /**
     * 获取用户画像缓存Key
     */
    public static String getUserProfileKey(Long userId) {
        return USER_PROFILE_PREFIX + userId;
    }
    
    /**
     * 获取验证码缓存Key
     */
    public static String getCaptchaKey(String captchaId) {
        return CAPTCHA_PREFIX + captchaId;
    }
    
    /**
     * 获取JWT黑名单缓存Key
     */
    public static String getJwtBlacklistKey(String token) {
        return JWT_BLACKLIST_PREFIX + token;
    }
}
