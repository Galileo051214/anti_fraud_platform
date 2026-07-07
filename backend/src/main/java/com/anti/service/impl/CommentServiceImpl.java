package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.common.HtmlSanitizer;
import com.anti.entity.Comment;
import com.anti.entity.CommentLike;
import com.anti.entity.ForumPost;
import com.anti.entity.User;
import com.anti.entity.dto.CreateCommentRequest;
import com.anti.entity.vo.CommentVO;
import com.anti.mapper.CommentLikeMapper;
import com.anti.mapper.CommentMapper;
import com.anti.mapper.ForumPostMapper;
import com.anti.mapper.UserMapper;
import com.anti.service.CommentService;
import com.anti.service.LeaderboardService;
import com.anti.service.ScoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 评论服务实现类
 */
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final ForumPostMapper forumPostMapper;
    private final UserMapper userMapper;
    private final ScoreService scoreService;
    private final LeaderboardService leaderboardService;

    public CommentServiceImpl(CommentMapper commentMapper,
                             CommentLikeMapper commentLikeMapper,
                             ForumPostMapper forumPostMapper,
                             UserMapper userMapper,
                             ScoreService scoreService,
                             LeaderboardService leaderboardService) {
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.forumPostMapper = forumPostMapper;
        this.userMapper = userMapper;
        this.scoreService = scoreService;
        this.leaderboardService = leaderboardService;
    }

    @Override
    @Transactional
    public CommentVO createComment(CreateCommentRequest request, Long userId) {
        String content = requireContent(request.getContent());
        ForumPost post = forumPostMapper.selectById(request.getPostId());
        if (!isAvailablePost(post)) {
            throw new BusinessException(404, "帖子不存在或不可评论");
        }

        Long parentId = request.getParentId() != null ? request.getParentId() : 0L;
        if (parentId < 0) {
            throw new BusinessException(400, "父评论ID不能小于0");
        }
        if (parentId > 0) {
            Comment parent = commentMapper.selectById(parentId);
            if (!isAvailableComment(parent)
                    || !request.getPostId().equals(parent.getPostId())) {
                throw new BusinessException(404, "父评论不存在");
            }
        }

        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setLikeCount(0);
        comment.setStatus(1);

        commentMapper.insert(comment);

        forumPostMapper.incrementCommentCount(request.getPostId());

        try {
            scoreService.addScore(userId, 1, "评论互动");
            leaderboardService.updateScore(userId, 1, "daily");
            leaderboardService.updateScore(userId, 1, "weekly");
            leaderboardService.updateScore(userId, 1, "all");
        } catch (Exception e) {
            // 积分发放失败不阻断评论
        }

        return convertToCommentVO(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (!isAvailableComment(comment)) {
            throw new BusinessException(404, "评论不存在");
        }

        User user = userMapper.selectById(userId);
        if (!Objects.equals(comment.getUserId(), userId) && (user == null || !"admin".equals(user.getRole()))) {
            throw new BusinessException(403, "无权限删除");
        }

        List<Comment> postComments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, comment.getPostId()));
        Set<Long> commentIds = new LinkedHashSet<>();
        collectCommentTreeIds(commentId, postComments, commentIds);

        commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                .in(CommentLike::getCommentId, commentIds));
        commentMapper.deleteBatchIds(commentIds);

        for (int i = 0; i < commentIds.size(); i++) {
            forumPostMapper.decrementCommentCount(comment.getPostId());
        }
    }

    private CommentVO convertToCommentVO(Comment comment) {
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
        return vo;
    }

    private void collectCommentTreeIds(Long commentId, List<Comment> postComments, Set<Long> commentIds) {
        if (!commentIds.add(commentId)) {
            return;
        }

        for (Comment comment : safeList(postComments)) {
            if (comment != null && commentId.equals(comment.getParentId())) {
                collectCommentTreeIds(comment.getId(), postComments, commentIds);
            }
        }
    }

    @Override
    @Transactional
    public void likeComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (!isAvailableComment(comment)) {
            throw new BusinessException(404, "评论不存在");
        }

        if (commentLikeMapper.existsByCommentIdAndUserId(commentId, userId)) {
            throw new BusinessException(400, "已点赞");
        }

        CommentLike commentLike = new CommentLike();
        commentLike.setCommentId(commentId);
        commentLike.setUserId(userId);
        commentLikeMapper.insert(commentLike);

        commentMapper.incrementLikeCount(commentId);
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (!isAvailableComment(comment)) {
            throw new BusinessException(404, "评论不存在");
        }

        int deleted = commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, userId));

        if (deleted > 0) {
            commentMapper.decrementLikeCount(commentId);
        }
    }

    private String requireContent(String content) {
        String normalized = content == null ? "" : content.trim();
        normalized = HtmlSanitizer.sanitizeCommentContent(normalized);
        if (normalized.isEmpty()) {
            throw new BusinessException(400, "评论内容不能为空");
        }
        if (normalized.length() > 2000) {
            throw new BusinessException(400, "评论内容不能超过2000个字符");
        }
        return normalized;
    }

    private boolean isAvailablePost(ForumPost post) {
        return post != null && Integer.valueOf(1).equals(post.getStatus());
    }

    private boolean isAvailableComment(Comment comment) {
        return comment != null && Integer.valueOf(1).equals(comment.getStatus());
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
