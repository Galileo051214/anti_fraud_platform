package com.anti.mapper;

import com.anti.entity.CommentLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 评论点赞Mapper接口
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    /**
     * 检查用户是否已点赞评论
     */
    @Select("SELECT COUNT(*) > 0 FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    boolean existsByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 统计评论点赞数
     */
    @Select("SELECT COUNT(*) FROM comment_like WHERE comment_id = #{commentId}")
    int countByCommentId(@Param("commentId") Long commentId);
}
