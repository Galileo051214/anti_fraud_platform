package com.anti.mapper;

import com.anti.entity.UserSimilarity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户相似度Mapper接口
 */
@Mapper
public interface UserSimilarityMapper extends BaseMapper<UserSimilarity> {

    /**
     * 查询与指定用户最相似的K个用户
     *
     * @param userId 用户ID
     * @param k      近邻数量
     * @return 相似用户列表
     */
    @Select("SELECT * FROM user_similarity " +
            "WHERE (user_id_a = #{userId} OR user_id_b = #{userId}) " +
            "AND similarity_score IS NOT NULL " +
            "ORDER BY similarity_score DESC " +
            "LIMIT #{k}")
    List<UserSimilarity> findTopKSimilarUsers(@Param("userId") Long userId, @Param("k") int k);

    /**
     * 查询与指定用户相似的所有用户(正向)
     *
     * @param userIdA 用户A ID
     * @param userIdB 用户B ID
     * @return 相似度记录
     */
    @Select("SELECT * FROM user_similarity WHERE user_id_a = #{userIdA} AND user_id_b = #{userIdB}")
    UserSimilarity findByUserPair(@Param("userIdA") Long userIdA, @Param("userIdB") Long userIdB);

    /**
     * 查询共同标签数量
     *
     * @param userIdA 用户A ID
     * @param userIdB 用户B ID
     * @return 共同标签数
     */
    @Select("SELECT COUNT(*) FROM user_behavior_matrix w1 " +
            "INNER JOIN user_behavior_matrix w2 ON w1.tag_id = w2.tag_id " +
            "WHERE w1.user_id = #{userIdA} AND w2.user_id = #{userIdB} " +
            "AND w1.behavior_score > 0 AND w2.behavior_score > 0")
    int countCommonTags(@Param("userIdA") Long userIdA, @Param("userIdB") Long userIdB);

    /**
     * 删除指定用户的所有相似度记录
     *
     * @param userId 用户ID
     */
    @Select("DELETE FROM user_similarity WHERE user_id_a = #{userId} OR user_id_b = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);
}
