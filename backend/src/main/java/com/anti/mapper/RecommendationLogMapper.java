package com.anti.mapper;

import com.anti.entity.RecommendationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 推荐记录日志Mapper接口
 */
@Mapper
public interface RecommendationLogMapper extends BaseMapper<RecommendationLog> {

    /**
     * 获取用户最近的推荐记录
     *
     * @param userId 用户ID
     * @param limit  数量限制
     * @return 推荐记录列表
     */
    @Select("SELECT * FROM recommendation_log WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<RecommendationLog> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 获取用户已推荐的案例ID列表(用于去重)
     *
     * @param userId 用户ID
     * @return 案例ID列表
     */
    @Select("SELECT item_id FROM recommendation_log WHERE user_id = #{userId} AND item_type = 'case'")
    List<Long> findRecommendedCaseIds(@Param("userId") Long userId);

    /**
     * 获取用户已推荐的资讯ID列表(用于去重)
     *
     * @param userId 用户ID
     * @return 资讯ID列表
     */
    @Select("SELECT item_id FROM recommendation_log WHERE user_id = #{userId} AND item_type = 'news'")
    List<Long> findRecommendedNewsIds(@Param("userId") Long userId);

    @Select("SELECT item_id FROM recommendation_log WHERE user_id = #{userId} AND item_type = 'challenge'")
    List<Long> findRecommendedChallengeIds(@Param("userId") Long userId);

    /**
     * 标记推荐记录为已点击
     *
     * @param id 推荐记录ID
     */
    @Update("UPDATE recommendation_log SET clicked = 1 WHERE id = #{id}")
    void markAsClicked(@Param("id") Long id);

    /**
     * 统计用户的推荐点击率
     *
     * @param userId 用户ID
     * @return 点击率
     */
    @Select("SELECT IFNULL(SUM(clicked) / COUNT(*), 0) FROM recommendation_log WHERE user_id = #{userId}")
    Double getClickRate(@Param("userId") Long userId);

    /**
     * 删除用户的旧推荐记录(保留最近N条)
     *
     * @param userId   用户ID
     * @param keepDays 保留天数
     */
    @Select("DELETE FROM recommendation_log WHERE user_id = #{userId} " +
            "AND create_time < DATE_SUB(NOW(), INTERVAL #{keepDays} DAY)")
    void deleteOldRecords(@Param("userId") Long userId, @Param("keepDays") int keepDays);
}
