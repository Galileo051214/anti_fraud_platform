package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.dto.CreateCommentRequest;
import com.anti.entity.vo.CommentVO;
import com.anti.service.CommentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/api/comment")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 创建评论
     */
    @PostMapping
    public Result<CommentVO> createComment(@RequestBody CreateCommentRequest request,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        CommentVO comment = commentService.createComment(request, userId);
        return Result.success(comment);
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        commentService.deleteComment(commentId, userId);
        return Result.success();
    }

    /**
     * 点赞评论
     */
    @PostMapping("/{commentId}/like")
    public Result<Void> likeComment(@PathVariable Long commentId,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        commentService.likeComment(commentId, userId);
        return Result.success();
    }

    /**
     * 取消点赞评论
     */
    @DeleteMapping("/{commentId}/like")
    public Result<Void> unlikeComment(@PathVariable Long commentId,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        commentService.unlikeComment(commentId, userId);
        return Result.success();
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
