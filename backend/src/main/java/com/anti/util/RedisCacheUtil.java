package com.anti.util;

import com.anti.common.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具类
 * 提供统一的缓存操作接口，封装常用的缓存操作方法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取缓存对象
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis GET error, key={}", key, e);
            return null;
        }
    }

    /**
     * 获取缓存对象(带泛型)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && clazz.isInstance(value)) {
                return (T) value;
            }
            return null;
        } catch (Exception e) {
            log.error("Redis GET error, key={}, class={}", key, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 设置缓存对象
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Redis SET error, key={}", key, e);
        }
    }

    /**
     * 设置缓存对象(带过期时间)
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("Redis SET error, key={}, timeout={}", key, timeout, e);
        }
    }

    /**
     * 设置缓存对象(使用秒为单位)
     */
    public void setWithSeconds(String key, Object value, long seconds) {
        set(key, value, seconds, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存对象(使用int类型的秒数)
     */
    public void set(String key, Object value, int timeoutSeconds) {
        set(key, value, (long) timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存对象(带long类型的过期时间秒数)
     */
    public void set(String key, Object value, long timeoutSeconds, boolean isSeconds) {
        if (isSeconds) {
            set(key, value, timeoutSeconds, TimeUnit.SECONDS);
        } else {
            set(key, value, (int) timeoutSeconds);
        }
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis DELETE error, key={}", key, e);
            return false;
        }
    }

    /**
     * 批量删除缓存
     */
    public Long delete(Collection<String> keys) {
        try {
            return redisTemplate.delete(keys);
        } catch (Exception e) {
            log.error("Redis batch DELETE error, keys={}", keys, e);
            return 0L;
        }
    }

    /**
     * 判断key是否存在
     */
    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis HAS_KEY error, key={}", key, e);
            return false;
        }
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis EXPIRE error, key={}, timeout={}", key, timeout, e);
            return false;
        }
    }

    /**
     * 获取过期时间(秒)
     */
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis GET_EXPIRE error, key={}", key, e);
            return -1L;
        }
    }

    /**
     * 递增
     */
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Redis INCREMENT error, key={}", key, e);
            return null;
        }
    }

    /**
     * 递增指定步长
     */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Redis INCREMENT error, key={}, delta={}", key, delta, e);
            return null;
        }
    }

    /**
     * 递减
     */
    public Long decrement(String key) {
        try {
            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.error("Redis DECREMENT error, key={}", key, e);
            return null;
        }
    }

    /**
     * 递减指定步长
     */
    public Long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.error("Redis DECREMENT error, key={}, delta={}", key, delta, e);
            return null;
        }
    }

    /**
     * 模糊匹配删除缓存
     */
    public Long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                return redisTemplate.delete(keys);
            }
            return 0L;
        } catch (Exception e) {
            log.error("Redis DELETE_BY_PATTERN error, pattern={}", pattern, e);
            return 0L;
        }
    }

    // ==================== 热点资讯缓存操作 ====================

    /**
     * 获取热点资讯缓存
     */
    public Object getHotNews(int limit) {
        return get(CacheConstants.getHotNewsKey(limit));
    }

    /**
     * 设置热点资讯缓存
     */
    public void setHotNews(int limit, Object data) {
        set(CacheConstants.getHotNewsKey(limit), data, CacheConstants.HOT_NEWS_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 删除热点资讯缓存
     */
    public void deleteHotNews() {
        deleteByPattern(CacheConstants.HOT_NEWS_PREFIX + "*");
    }

    // ==================== 热点案例缓存操作 ====================

    /**
     * 获取热点案例缓存
     */
    public Object getHotCases(int limit) {
        return get(CacheConstants.getHotCaseKey(limit));
    }

    /**
     * 设置热点案例缓存
     */
    public void setHotCases(int limit, Object data) {
        set(CacheConstants.getHotCaseKey(limit), data, CacheConstants.HOT_CASE_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 删除热点案例缓存
     */
    public void deleteHotCases() {
        deleteByPattern(CacheConstants.HOT_CASE_PREFIX + "*");
    }

    // ==================== 紧急预警缓存操作 ====================

    /**
     * 获取紧急预警缓存
     */
    public Object getEmergencyAlert(String alertType) {
        return get(CacheConstants.getEmergencyAlertKey(alertType));
    }

    /**
     * 设置紧急预警缓存
     */
    public void setEmergencyAlert(String alertType, Object data) {
        set(CacheConstants.getEmergencyAlertKey(alertType), data, CacheConstants.EMERGENCY_ALERT_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 删除紧急预警缓存
     */
    public void deleteEmergencyAlert() {
        deleteByPattern(CacheConstants.EMERGENCY_ALERT_PREFIX + "*");
    }

    // ==================== 推荐结果缓存操作 ====================

    /**
     * 获取推荐结果缓存
     */
    public Object getRecommendations(Long userId, int limit) {
        return get(CacheConstants.getRecommendKey(userId, limit));
    }

    /**
     * 设置推荐结果缓存
     */
    public void setRecommendations(Long userId, int limit, Object data) {
        set(CacheConstants.getRecommendKey(userId, limit), data, CacheConstants.RECOMMEND_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 删除用户推荐结果缓存
     */
    public void deleteUserRecommendations(Long userId) {
        deleteByPattern(CacheConstants.RECOMMEND_PREFIX + userId + ":*");
    }

    /**
     * 删除所有推荐结果缓存
     */
    public void deleteAllRecommendations() {
        deleteByPattern(CacheConstants.RECOMMEND_PREFIX + "*");
    }

    // ==================== 用户兴趣缓存操作 ====================

    /**
     * 获取用户兴趣缓存
     */
    public Object getUserInterest(Long userId) {
        return get(CacheConstants.getUserInterestKey(userId));
    }

    /**
     * 设置用户兴趣缓存
     */
    public void setUserInterest(Long userId, Object data) {
        set(CacheConstants.getUserInterestKey(userId), data, CacheConstants.USER_INTEREST_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 删除用户兴趣缓存
     */
    public void deleteUserInterest(Long userId) {
        delete(CacheConstants.getUserInterestKey(userId));
    }

    // ==================== 排行榜缓存操作 ====================

    /**
     * 获取日排行榜缓存
     */
    public Object getDailyLeaderboard(int limit) {
        return get(CacheConstants.getDailyLeaderboardKey(limit));
    }

    /**
     * 设置日排行榜缓存
     */
    public void setDailyLeaderboard(int limit, Object data) {
        set(CacheConstants.getDailyLeaderboardKey(limit), data, CacheConstants.DAILY_LEADERBOARD_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 获取周排行榜缓存
     */
    public Object getWeeklyLeaderboard(int limit) {
        return get(CacheConstants.getWeeklyLeaderboardKey(limit));
    }

    /**
     * 设置周排行榜缓存
     */
    public void setWeeklyLeaderboard(int limit, Object data) {
        set(CacheConstants.getWeeklyLeaderboardKey(limit), data, CacheConstants.WEEKLY_LEADERBOARD_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 获取总排行榜缓存
     */
    public Object getAllTimeLeaderboard(int limit) {
        return get(CacheConstants.getAllTimeLeaderboardKey(limit));
    }

    /**
     * 设置总排行榜缓存
     */
    public void setAllTimeLeaderboard(int limit, Object data) {
        set(CacheConstants.getAllTimeLeaderboardKey(limit), data, CacheConstants.ALLTIME_LEADERBOARD_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 获取用户排名缓存
     */
    public Object getUserRank(Long userId, String periodType) {
        return get(CacheConstants.getUserRankKey(userId, periodType));
    }

    /**
     * 设置用户排名缓存
     */
    public void setUserRank(Long userId, String periodType, Object data) {
        set(CacheConstants.getUserRankKey(userId, periodType), data, CacheConstants.DAILY_LEADERBOARD_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 删除所有排行榜缓存
     */
    public void deleteAllLeaderboard() {
        deleteByPattern(CacheConstants.LEADERBOARD_PREFIX + "*");
    }

    // ==================== 资讯/案例详情缓存操作 ====================

    /**
     * 获取资讯详情缓存
     */
    public Object getNewsDetail(Long newsId) {
        return get(CacheConstants.getNewsDetailKey(newsId));
    }

    /**
     * 设置资讯详情缓存
     */
    public void setNewsDetail(Long newsId, Object data) {
        set(CacheConstants.getNewsDetailKey(newsId), data, CacheConstants.NEWS_DETAIL_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 删除资讯详情缓存
     */
    public void deleteNewsDetail(Long newsId) {
        delete(CacheConstants.getNewsDetailKey(newsId));
    }

    /**
     * 获取案例详情缓存
     */
    public Object getCaseDetail(Long caseId) {
        return get(CacheConstants.getCaseDetailKey(caseId));
    }

    /**
     * 设置案例详情缓存
     */
    public void setCaseDetail(Long caseId, Object data) {
        set(CacheConstants.getCaseDetailKey(caseId), data, CacheConstants.CASE_DETAIL_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 删除案例详情缓存
     */
    public void deleteCaseDetail(Long caseId) {
        delete(CacheConstants.getCaseDetailKey(caseId));
    }

    // ==================== 用户会话缓存操作 ====================

    /**
     * 获取用户会话缓存
     */
    public Object getUserSession(Long userId) {
        return get(CacheConstants.getUserSessionKey(userId));
    }

    /**
     * 设置用户会话缓存
     */
    public void setUserSession(Long userId, Object data) {
        set(CacheConstants.getUserSessionKey(userId), data, CacheConstants.USER_SESSION_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 删除用户会话缓存
     */
    public void deleteUserSession(Long userId) {
        delete(CacheConstants.getUserSessionKey(userId));
    }

    // ==================== 统计数据缓存操作 ====================

    /**
     * 获取统计数据缓存
     */
    public Object getStatistics(String type) {
        return get(CacheConstants.getStatisticsKey(type));
    }

    /**
     * 设置统计数据缓存
     */
    public void setStatistics(String type, Object data) {
        set(CacheConstants.getStatisticsKey(type), data, CacheConstants.STATISTICS_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 删除所有统计数据缓存
     */
    public void deleteAllStatistics() {
        deleteByPattern(CacheConstants.STATISTICS_PREFIX + "*");
    }

    // ==================== JWT黑名单缓存操作 ====================

    /**
     * 检查Token是否在黑名单中
     */
    public Boolean isTokenBlacklisted(String token) {
        return hasKey(CacheConstants.getJwtBlacklistKey(token));
    }

    /**
     * 将Token加入黑名单
     */
    public void addTokenToBlacklist(String token, long expireSeconds) {
        set(CacheConstants.getJwtBlacklistKey(token), "1", expireSeconds, TimeUnit.SECONDS);
    }

    /**
     * 从黑名单移除Token
     */
    public void removeTokenFromBlacklist(String token) {
        delete(CacheConstants.getJwtBlacklistKey(token));
    }

    // ==================== 验证码缓存操作 ====================

    /**
     * 获取验证码
     */
    public Object getCaptcha(String captchaId) {
        return get(CacheConstants.getCaptchaKey(captchaId));
    }

    /**
     * 设置验证码
     */
    public void setCaptcha(String captchaId, String code) {
        set(CacheConstants.getCaptchaKey(captchaId), code, CacheConstants.CAPTCHA_CACHE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 删除验证码
     */
    public void deleteCaptcha(String captchaId) {
        delete(CacheConstants.getCaptchaKey(captchaId));
    }

    /**
     * 验证验证码是否正确
     */
    public boolean verifyCaptcha(String captchaId, String code) {
        Object cached = get(CacheConstants.getCaptchaKey(captchaId));
        if (cached != null && cached.toString().equalsIgnoreCase(code)) {
            deleteCaptcha(captchaId);
            return true;
        }
        return false;
    }

    // ==================== 分布式锁操作 ====================

    /**
     * 尝试获取锁
     */
    public Boolean tryLock(String lockKey, String lockValue, long expireSeconds) {
        try {
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();
            return ops.setIfAbsent(lockKey, lockValue, expireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis TRY_LOCK error, key={}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放锁
     */
    public Boolean releaseLock(String lockKey, String lockValue) {
        try {
            Object currentValue = get(lockKey);
            if (lockValue.equals(currentValue)) {
                return delete(lockKey);
            }
            return false;
        } catch (Exception e) {
            log.error("Redis RELEASE_LOCK error, key={}", lockKey, e);
            return false;
        }
    }
}
