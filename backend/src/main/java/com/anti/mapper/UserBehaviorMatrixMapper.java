package com.anti.mapper;

import com.anti.entity.UserBehaviorMatrix;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户行为矩阵Mapper接口
 */
@Mapper
public interface UserBehaviorMatrixMapper extends BaseMapper<UserBehaviorMatrix> {

    /**
     * 获取用户的所有标签得分
     *
     * @param userId 用户ID
     * @return 行为矩阵列表
     */
    @Select("SELECT * FROM user_behavior_matrix WHERE user_id = #{userId} AND behavior_score > 0")
    List<UserBehaviorMatrix> findByUserId(@Param("userId") Long userId);

    /**
     * 获取用户对指定标签的得分
     *
     * @param userId 用户ID
     * @param tagId  标签ID
     * @return 行为矩阵记录
     */
    @Select("SELECT * FROM user_behavior_matrix WHERE user_id = #{userId} AND tag_id = #{tagId}")
    UserBehaviorMatrix findByUserAndTag(@Param("userId") Long userId, @Param("tagId") Long tagId);

    /**
     * 获取所有用户的标签得分(用于批量计算相似度)
     *
     * @return 所有行为矩阵记录
     */
    @Select("SELECT * FROM user_behavior_matrix WHERE behavior_score > 0 ORDER BY user_id")
    List<UserBehaviorMatrix> findAllActive();

    /**
     * 获取指定用户的Top N兴趣标签
     *
     * @param userId 用户ID
     * @param limit 数量限制
     * @return 行为矩阵列表
     */
    @Select("SELECT * FROM user_behavior_matrix WHERE user_id = #{userId} " +
            "AND behavior_score > 0 ORDER BY behavior_score DESC LIMIT #{limit}")
    List<UserBehaviorMatrix> findTopTagsByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 删除用户的所有行为记录
     *
     * @param userId 用户ID
     */
    @Select("DELETE FROM user_behavior_matrix WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);
}
