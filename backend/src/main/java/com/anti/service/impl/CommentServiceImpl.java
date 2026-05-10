package com.anti.service.impl;

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
        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        comment.setContent(request.getContent());
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
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        User user = userMapper.selectById(userId);
        if (!comment.getUserId().equals(userId) && (user == null || !"admin".equals(user.getRole()))) {
            throw new RuntimeException("无权限删除");
        }

        commentMapper.deleteById(commentId);

        forumPostMapper.decrementCommentCount(comment.getPostId());
    }

    @Override
    @Transactional
    public void likeComment(Long commentId, Long userId) {
        if (commentLikeMapper.existsByCommentIdAndUserId(commentId, userId)) {
            throw new RuntimeException("已点赞");
        }

        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
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
        commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, userId));

        commentMapper.decrementLikeCount(commentId);
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
}
