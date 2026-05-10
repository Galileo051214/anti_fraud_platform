package com.anti.mapper;

import com.anti.entity.PostLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 帖子点赞Mapper接口
 */
@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {

    /**
     * 检查用户是否已点赞
     */
    @Select("SELECT COUNT(*) > 0 FROM post_like WHERE post_id = #{postId} AND user_id = #{userId}")
    boolean existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 统计帖子点赞数
     */
    @Select("SELECT COUNT(*) FROM post_like WHERE post_id = #{postId}")
    int countByPostId(@Param("postId") Long postId);
}
