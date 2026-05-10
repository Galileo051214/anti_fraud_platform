package com.anti.service;

import com.anti.entity.dto.CreateCommentRequest;
import com.anti.entity.vo.CommentVO;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 创建评论
     *
     * @param request 评论请求
     * @param userId  用户ID
     * @return 创建的评论
     */
    CommentVO createComment(CreateCommentRequest request, Long userId);

    /**
     * 删除评论
     *
     * @param commentId 评论ID
     * @param userId    用户ID
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * 点赞评论
     *
     * @param commentId 评论ID
     * @param userId    用户ID
     */
    void likeComment(Long commentId, Long userId);

    /**
     * 取消点赞评论
     *
     * @param commentId 评论ID
     * @param userId    用户ID
     */
    void unlikeComment(Long commentId, Long userId);
}
