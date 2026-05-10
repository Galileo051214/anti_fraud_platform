package com.anti.service.impl;

import com.anti.entity.*;
import com.anti.entity.dto.CreatePostRequest;
import com.anti.entity.dto.UpdatePostRequest;
import com.anti.entity.vo.CommentVO;
import com.anti.entity.vo.PostVO;
import com.anti.mapper.*;
import com.anti.service.AchievementService;
import com.anti.service.ForumPostService;
import com.anti.service.LeaderboardService;
import com.anti.service.ScoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 论坛帖子服务实现类
 */
@Service
public class ForumPostServiceImpl extends ServiceImpl<ForumPostMapper, ForumPost> implements ForumPostService {

    private final ForumPostMapper forumPostMapper;
    private final PostLikeMapper postLikeMapper;
    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;
    private final AchievementService achievementService;
    private final ScoreService scoreService;
    private final LeaderboardService leaderboardService;

    public ForumPostServiceImpl(ForumPostMapper forumPostMapper,
                                PostLikeMapper postLikeMapper,
                                CommentMapper commentMapper,
                                CommentLikeMapper commentLikeMapper,
                                UserMapper userMapper,
                                AchievementService achievementService,
                                ScoreService scoreService,
                                LeaderboardService leaderboardService) {
        this.forumPostMapper = forumPostMapper;
        this.postLikeMapper = postLikeMapper;
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.userMapper = userMapper;
        this.achievementService = achievementService;
        this.scoreService = scoreService;
        this.leaderboardService = leaderboardService;
    }

    @Override
    public IPage<PostVO> getPostPage(int pageNum, int pageSize, String postType, String sortBy, String keyword, Long userId) {
        Page<ForumPost> page = new Page<>(pageNum, pageSize);
        IPage<ForumPost> postPage;

        if (keyword != null && !keyword.isBlank()) {
            postPage = forumPostMapper.searchByKeyword(page, keyword);
        } else if ("like".equals(sortBy)) {
            postPage = forumPostMapper.selectByTypeOrderByLike(page, postType);
        } else if ("comment".equals(sortBy)) {
            postPage = forumPostMapper.selectByTypeOrderByComment(page, postType);
        } else {
            postPage = forumPostMapper.selectByTypeOrderByTime(page, postType);
        }

        return convertToPostVOPage(postPage, userId);
    }

    @Override
    public PostVO getPostDetail(Long postId, Long userId) {
        ForumPost post = getById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }

        forumPostMapper.incrementViewCount(postId);

        PostVO postVO = convertToPostVO(post);
        if (userId != null) {
            postVO.setIsLiked(postLikeMapper.existsByPostIdAndUserId(postId, userId));
        }

        return postVO;
    }

    @Override
    @Transactional
    public PostVO createPost(CreatePostRequest request, Long authorId) {
        ForumPost post = new ForumPost();
        post.setUserId(authorId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setPostType(request.getPostType() != null ? request.getPostType() : "experience");
        post.setTagIds(request.getTagIds());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setIsFeatured(0);
        post.setIsTop(0);
        post.setStatus(1);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());

        save(post);
        try {
            scoreService.addScore(authorId, 3, "发帖分享");
            leaderboardService.updateScore(authorId, 3, "daily");
            leaderboardService.updateScore(authorId, 3, "weekly");
            leaderboardService.updateScore(authorId, 3, "all");
        } catch (Exception e) {
            // 积分发放失败不阻断发帖
        }
        try {
            long postCount = count(new LambdaQueryWrapper<ForumPost>()
                    .eq(ForumPost::getUserId, authorId)
                    .eq(ForumPost::getStatus, 1));
            achievementService.checkAndUnlockAchievements(authorId, "post_count", Math.toIntExact(Math.min(postCount, Integer.MAX_VALUE)));
            achievementService.refreshContinuousLearningStreak(authorId);
        } catch (Exception e) {
            // 发帖已成功，成就校验失败不阻断
        }
        return convertToPostVO(post);
    }

    @Override
    @Transactional
    public PostVO updatePost(Long postId, UpdatePostRequest request, Long userId) {
        ForumPost post = getById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("无权限修改");
        }

        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getContent() != null) post.setContent(request.getContent());
        if (request.getPostType() != null) post.setPostType(request.getPostType());
        if (request.getTagIds() != null) post.setTagIds(request.getTagIds());
        if (request.getStatus() != null) post.setStatus(request.getStatus());
        if (request.getIsFeatured() != null) post.setIsFeatured(request.getIsFeatured());
        if (request.getIsTop() != null) post.setIsTop(request.getIsTop());
        post.setUpdateTime(LocalDateTime.now());

        updateById(post);
        return convertToPostVO(post);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        ForumPost post = getById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        if (!post.getUserId().equals(userId)) {
            User user = userMapper.selectById(userId);
            if (user == null || !"admin".equals(user.getRole())) {
                throw new RuntimeException("无权限删除");
            }
        }

        // 删除帖子点赞
        postLikeMapper.delete(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId));

        // 查询并删除所有评论（包含回复）
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, postId));
        if (comments != null && !comments.isEmpty()) {
            List<Long> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toList());

            // 删除评论点赞
            commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                    .in(CommentLike::getCommentId, commentIds));

            // 删除评论
            commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, postId));
        }

        // 删除帖子
        removeById(postId);
    }

    @Override
    @Transactional
    public void likePost(Long postId, Long userId) {
        if (postLikeMapper.existsByPostIdAndUserId(postId, userId)) {
            throw new RuntimeException("已点赞");
        }

        ForumPost post = getById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }

        PostLike postLike = new PostLike();
        postLike.setPostId(postId);
        postLike.setUserId(userId);
        postLike.setCreateTime(LocalDateTime.now());
        postLikeMapper.insert(postLike);

        post.setLikeCount(post.getLikeCount() + 1);
        updateById(post);
    }

    @Override
    @Transactional
    public void unlikePost(Long postId, Long userId) {
        postLikeMapper.delete(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId));

        ForumPost post = getById(postId);
        if (post != null && post.getLikeCount() > 0) {
            post.setLikeCount(post.getLikeCount() - 1);
            updateById(post);
        }
    }

    @Override
    @Transactional
    public void setTop(Long postId, int isTop) {
        ForumPost post = getById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        post.setIsTop(isTop);
        updateById(post);
    }

    @Override
    @Transactional
    public void setFeatured(Long postId, int isFeatured) {
        ForumPost post = getById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        post.setIsFeatured(isFeatured);
        updateById(post);
    }

    @Override
    public List<CommentVO> getPostComments(Long postId, Long userId) {
        List<Comment> allComments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .eq(Comment::getStatus, 1)
                        .orderByAsc(Comment::getCreateTime)
        );

        ForumPost post = getById(postId);

        Map<Long, List<CommentVO>> groupedComments = allComments.stream()
                .map(c -> convertToCommentVO(c, userId, post != null ? post.getUserId() : null))
                .collect(Collectors.groupingBy(CommentVO::getParentId));

        return buildCommentTree(0L, groupedComments);
    }

    @Override
    public IPage<PostVO> getUserPosts(Long userId, int pageNum, int pageSize) {
        Page<ForumPost> page = new Page<>(pageNum, pageSize);
        IPage<ForumPost> postPage = page(page, new LambdaQueryWrapper<ForumPost>()
                .eq(ForumPost::getUserId, userId)
                .eq(ForumPost::getStatus, 1)
                .orderByDesc(ForumPost::getCreateTime));

        return convertToPostVOPage(postPage, userId);
    }

    private List<CommentVO> buildCommentTree(Long parentId, Map<Long, List<CommentVO>> groupedComments) {
        List<CommentVO> tree = groupedComments.getOrDefault(parentId, Collections.emptyList());
        for (CommentVO comment : tree) {
            List<CommentVO> children = buildCommentTree(comment.getId(), groupedComments);
            comment.setChildren(children);
        }
        return tree;
    }

    private IPage<PostVO> convertToPostVOPage(IPage<ForumPost> postPage, Long userId) {
        List<PostVO> voList = postPage.getRecords().stream()
                .map(p -> convertToPostVOWithLikeStatus(p, userId))
                .collect(Collectors.toList());

        Page<PostVO> resultPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    private PostVO convertToPostVO(ForumPost post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setPostType(post.getPostType());
        vo.setTagIds(post.getTagIds());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setIsFeatured(post.getIsFeatured());
        vo.setIsTop(post.getIsTop());
        vo.setStatus(post.getStatus());
        vo.setAuthorName(post.getAuthorName());
        vo.setAuthorAvatar(post.getAuthorAvatar());
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());
        return vo;
    }

    private PostVO convertToPostVOWithLikeStatus(ForumPost post, Long userId) {
        PostVO vo = convertToPostVO(post);
        if (userId != null) {
            vo.setIsLiked(postLikeMapper.existsByPostIdAndUserId(post.getId(), userId));
        }
        return vo;
    }

    private CommentVO convertToCommentVO(Comment comment, Long userId, Long postAuthorId) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setUserId(comment.getUserId());
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount());
        vo.setStatus(comment.getStatus());
        vo.setAuthorName(comment.getAuthorName());
        vo.setAuthorAvatar(comment.getAuthorAvatar());
        vo.setCreateTime(comment.getCreateTime());

        if (userId != null) {
            vo.setIsLiked(commentLikeMapper.existsByCommentIdAndUserId(comment.getId(), userId));
        }

        if (postAuthorId != null && comment.getUserId() != null) {
            vo.setIsAuthor(comment.getUserId().equals(postAuthorId));
        }

        return vo;
    }
}
