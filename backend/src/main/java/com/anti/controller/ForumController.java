package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.dto.CreatePostRequest;
import com.anti.entity.dto.UpdatePostRequest;
import com.anti.entity.vo.CommentVO;
import com.anti.entity.vo.PostVO;
import com.anti.service.ForumPostService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 论坛帖子控制器
 */
@RestController
@RequestMapping("/api/forum")
public class ForumController {

    private final ForumPostService forumPostService;

    public ForumController(ForumPostService forumPostService) {
        this.forumPostService = forumPostService;
    }

    /**
     * 分页查询帖子列表
     */
    @GetMapping("/post/page")
    public Result<IPage<PostVO>> getPostPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String postType,
            @RequestParam(defaultValue = "time") String sortBy,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        IPage<PostVO> page = forumPostService.getPostPage(pageNum, pageSize, postType, sortBy, keyword, userId);
        return Result.success(page);
    }

    /**
     * 获取帖子详情
     */
    @GetMapping("/post/{postId}")
    public Result<PostVO> getPostDetail(@PathVariable Long postId,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        PostVO post = forumPostService.getPostDetail(postId, userId);
        return Result.success(post);
    }

    /**
     * 创建帖子
     */
    @PostMapping("/post")
    public Result<PostVO> createPost(@RequestBody CreatePostRequest request,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        PostVO post = forumPostService.createPost(request, userId);
        return Result.success(post);
    }

    /**
     * 更新帖子
     */
    @PutMapping("/post/{postId}")
    public Result<PostVO> updatePost(@PathVariable Long postId,
                                    @RequestBody UpdatePostRequest request,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        PostVO post = forumPostService.updatePost(postId, request, userId);
        return Result.success(post);
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/post/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        forumPostService.deletePost(postId, userId);
        return Result.success();
    }

    /**
     * 点赞帖子
     */
    @PostMapping("/post/{postId}/like")
    public Result<Void> likePost(@PathVariable Long postId,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        forumPostService.likePost(postId, userId);
        return Result.success();
    }

    /**
     * 取消点赞帖子
     */
    @DeleteMapping("/post/{postId}/like")
    public Result<Void> unlikePost(@PathVariable Long postId,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        forumPostService.unlikePost(postId, userId);
        return Result.success();
    }

    /**
     * 置顶帖子(管理员)
     */
    @PostMapping("/post/{postId}/top")
    public Result<Void> setTop(@PathVariable Long postId,
                               @RequestParam(defaultValue = "1") int isTop) {
        forumPostService.setTop(postId, isTop);
        return Result.success();
    }

    /**
     * 精选帖子(管理员)
     */
    @PostMapping("/post/{postId}/featured")
    public Result<Void> setFeatured(@PathVariable Long postId,
                                    @RequestParam(defaultValue = "1") int isFeatured) {
        forumPostService.setFeatured(postId, isFeatured);
        return Result.success();
    }

    /**
     * 获取帖子的评论列表
     */
    @GetMapping("/post/{postId}/comments")
    public Result<List<CommentVO>> getPostComments(@PathVariable Long postId,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        List<CommentVO> comments = forumPostService.getPostComments(postId, userId);
        return Result.success(comments);
    }

    /**
     * 获取用户发布的帖子
     */
    @GetMapping("/user/{userId}/posts")
    public Result<IPage<PostVO>> getUserPosts(@PathVariable Long userId,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        IPage<PostVO> page = forumPostService.getUserPosts(userId, pageNum, pageSize);
        return Result.success(page);
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
