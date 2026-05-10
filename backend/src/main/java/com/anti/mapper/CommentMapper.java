package com.anti.mapper;

import com.anti.entity.Comment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 评论Mapper接口
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 查询帖子的所有评论(树形结构用)
     */
    IPage<Comment> selectByPostId(Page<Comment> page, @Param("postId") Long postId);

    /**
     * 查询评论的回复
     */
    @Select("SELECT * FROM comment WHERE parent_id = #{parentId} AND status = 1 ORDER BY create_time ASC")
    java.util.List<Comment> selectRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * 增加点赞数
     */
    @Select("UPDATE comment SET like_count = like_count + 1 WHERE id = #{commentId}")
    void incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 减少点赞数
     */
    @Select("UPDATE comment SET like_count = like_count - 1 WHERE id = #{commentId} AND like_count > 0")
    void decrementLikeCount(@Param("commentId") Long commentId);
}
