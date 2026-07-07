package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.common.HtmlSanitizer;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 论坛帖子服务实现类
 */
@Service
public class ForumPostServiceImpl extends ServiceImpl<ForumPostMapper, ForumPost> implements ForumPostService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_IMAGE_COUNT = 9;
    private static final int MAX_IMAGE_URL_LENGTH = 500;
    private static final String LOCAL_UPLOAD_IMAGE_PREFIX = "/uploads/images/";
    private static final Set<String> ALLOWED_POST_TYPES = Set.of("experience", "question", "discussion");
    private static final Set<String> HTTP_SCHEMES = Set.of("http", "https");

    @Value("${upload.base-url:http://localhost:8080}")
    private String uploadBaseUrl = "http://localhost:8080";

    @Value("${forum.post-images.allowed-upload-domains:}")
    private String allowedUploadDomains = "";

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
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        String normalizedPostType = normalizePostType(postType, true);
        String normalizedKeyword = trimToNull(keyword);
        String normalizedSortBy = normalizeSortBy(sortBy);
        Page<ForumPost> page = new Page<>(safePageNum, safePageSize);
        IPage<ForumPost> postPage;

        if (normalizedKeyword != null) {
            postPage = forumPostMapper.searchByKeyword(page, normalizedKeyword, normalizedPostType);
        } else if ("like".equals(normalizedSortBy)) {
            postPage = forumPostMapper.selectByTypeOrderByLike(page, normalizedPostType);
        } else if ("comment".equals(normalizedSortBy)) {
            postPage = forumPostMapper.selectByTypeOrderByComment(page, normalizedPostType);
        } else {
            postPage = forumPostMapper.selectByTypeOrderByTime(page, normalizedPostType);
        }

        return convertToPostVOPage(postPage, userId);
    }

    @Override
    public PostVO getPostDetail(Long postId, Long userId) {
        ForumPost post = getById(postId);
        if (!isAvailablePost(post)) {
            throw new BusinessException(404, "帖子不存在");
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
        String title = requireText(request.getTitle(), "帖子标题", 100);
        String rawContent = requireText(request.getContent(), "帖子内容", 10000);
        String content = HtmlSanitizer.sanitizePostContent(rawContent);
        if (content.isEmpty()) {
            throw new BusinessException(400, "帖子内容无效（仅包含非法标签）");
        }
        String postType = normalizePostType(request.getPostType(), false);

        ForumPost post = new ForumPost();
        post.setUserId(authorId);
        post.setTitle(title);
        post.setContent(content);
        post.setPostType(postType);
        post.setTagIds(request.getTagIds());
        post.setImageUrls(normalizeImageUrls(request.getImageUrls() == null
                ? Collections.emptyList()
                : request.getImageUrls()));
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
            throw new BusinessException(404, "帖子不存在");
        }
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限修改");
        }

        if (request.getTitle() != null) post.setTitle(requireText(request.getTitle(), "帖子标题", 100));
        if (request.getContent() != null) {
            String sanitized = HtmlSanitizer.sanitizePostContent(requireText(request.getContent(), "帖子内容", 10000));
            if (sanitized.isEmpty()) {
                throw new BusinessException(400, "帖子内容无效（仅包含非法标签）");
            }
            post.setContent(sanitized);
        }
        if (request.getPostType() != null) post.setPostType(normalizePostType(request.getPostType(), false));
        if (request.getTagIds() != null) post.setTagIds(request.getTagIds());
        if (request.getImageUrls() != null) post.setImageUrls(normalizeImageUrls(request.getImageUrls()));
        post.setUpdateTime(LocalDateTime.now());

        updateById(post);
        return convertToPostVO(post);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        ForumPost post = getById(postId);
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        if (!post.getUserId().equals(userId)) {
            User user = userMapper.selectById(userId);
            if (user == null || !"admin".equals(user.getRole())) {
                throw new BusinessException(403, "无权限删除");
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
        ForumPost post = getById(postId);
        if (!isAvailablePost(post)) {
            throw new BusinessException(404, "帖子不存在");
        }
        if (postLikeMapper.existsByPostIdAndUserId(postId, userId)) {
            throw new BusinessException(400, "已点赞");
        }

        PostLike postLike = new PostLike();
        postLike.setPostId(postId);
        postLike.setUserId(userId);
        postLike.setCreateTime(LocalDateTime.now());
        postLikeMapper.insert(postLike);

        post.setLikeCount(defaultInt(post.getLikeCount()) + 1);
        updateById(post);
    }

    @Override
    @Transactional
    public void unlikePost(Long postId, Long userId) {
        ForumPost post = getById(postId);
        if (!isAvailablePost(post)) {
            throw new BusinessException(404, "帖子不存在");
        }

        int deleted = postLikeMapper.delete(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId));

        if (deleted > 0 && defaultInt(post.getLikeCount()) > 0) {
            post.setLikeCount(defaultInt(post.getLikeCount()) - 1);
            updateById(post);
        }
    }

    @Override
    @Transactional
    public void setTop(Long postId, int isTop) {
        validateSwitchValue(isTop, "是否置顶");
        ForumPost post = getById(postId);
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        post.setIsTop(isTop);
        updateById(post);
    }

    @Override
    @Transactional
    public void setFeatured(Long postId, int isFeatured) {
        validateSwitchValue(isFeatured, "是否精选");
        ForumPost post = getById(postId);
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        post.setIsFeatured(isFeatured);
        updateById(post);
    }

    @Override
    public List<CommentVO> getPostComments(Long postId, Long userId) {
        ForumPost post = getById(postId);
        if (!isAvailablePost(post)) {
            throw new BusinessException(404, "帖子不存在");
        }

        List<Comment> allComments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .eq(Comment::getStatus, 1)
                        .orderByAsc(Comment::getCreateTime)
        );

        Map<Long, List<CommentVO>> groupedComments = safeList(allComments).stream()
                .map(c -> convertToCommentVO(c, userId, post != null ? post.getUserId() : null))
                .collect(Collectors.groupingBy(CommentVO::getParentId));

        return buildCommentTree(0L, groupedComments);
    }

    @Override
    public IPage<PostVO> getUserPosts(Long userId, int pageNum, int pageSize) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(400, "用户ID无效");
        }
        Page<ForumPost> page = new Page<>(Math.max(1, pageNum), Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE)));
        IPage<ForumPost> postPage = page(page, new LambdaQueryWrapper<ForumPost>()
                .eq(ForumPost::getUserId, userId)
                .eq(ForumPost::getStatus, 1)
                .orderByDesc(ForumPost::getCreateTime));

        return convertToPostVOPage(postPage, userId);
    }

    private void validateSwitchValue(int value, String fieldName) {
        if (value != 0 && value != 1) {
            throw new BusinessException(400, fieldName + "只能为0或1");
        }
    }

    private String normalizePostType(String postType, boolean allowBlank) {
        String normalized = trimToNull(postType);
        if (normalized == null) {
            if (allowBlank) {
                return null;
            }
            return "experience";
        }
        if (!ALLOWED_POST_TYPES.contains(normalized)) {
            throw new BusinessException(400, "帖子类型只能是experience、question或discussion");
        }
        return normalized;
    }

    private String normalizeSortBy(String sortBy) {
        String normalized = trimToNull(sortBy);
        if (normalized == null) {
            return "time";
        }
        if (!Set.of("time", "like", "comment").contains(normalized)) {
            throw new BusinessException(400, "排序方式只能是time、like或comment");
        }
        return normalized;
    }

    private String requireText(String value, String fieldName, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(400, fieldName + "不能为空");
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(400, fieldName + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(400, "帖子图片最多9张");
        }

        List<String> normalizedUrls = new ArrayList<>(imageUrls.size());
        for (String imageUrl : imageUrls) {
            String normalizedUrl = trimToNull(imageUrl);
            if (normalizedUrl == null) {
                throw new BusinessException(400, "图片URL不能为空");
            }
            if (normalizedUrl.length() > MAX_IMAGE_URL_LENGTH) {
                throw new BusinessException(400, "图片URL不能超过500个字符");
            }
            if (!isAllowedImageUrl(normalizedUrl)) {
                throw new BusinessException(400, "图片URL只能使用本站/uploads/images/路径或已配置上传域名");
            }
            normalizedUrls.add(normalizedUrl);
        }
        return normalizedUrls;
    }

    private boolean isAllowedImageUrl(String imageUrl) {
        if (imageUrl.startsWith(LOCAL_UPLOAD_IMAGE_PREFIX)) {
            return true;
        }

        URI uri;
        try {
            uri = new URI(imageUrl);
        } catch (URISyntaxException e) {
            return false;
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        String path = uri.getPath();
        if (scheme == null || host == null || path == null) {
            return false;
        }
        if (!HTTP_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!path.startsWith(LOCAL_UPLOAD_IMAGE_PREFIX)) {
            return false;
        }
        return allowedUploadHostPorts().contains(hostPort(uri));
    }

    private Set<String> allowedUploadHostPorts() {
        Set<String> hostPorts = new HashSet<>();
        addAllowedUploadHostPorts(hostPorts, uploadBaseUrl);
        addAllowedUploadHostPorts(hostPorts, allowedUploadDomains);
        return hostPorts;
    }

    private void addAllowedUploadHostPorts(Set<String> hostPorts, String configuredDomains) {
        String normalizedConfig = trimToNull(configuredDomains);
        if (normalizedConfig == null) {
            return;
        }
        Arrays.stream(normalizedConfig.split(","))
                .map(this::normalizeAllowedUploadHostPort)
                .filter(Objects::nonNull)
                .forEach(hostPorts::add);
    }

    private String normalizeAllowedUploadHostPort(String configuredDomain) {
        String normalizedDomain = trimToNull(configuredDomain);
        if (normalizedDomain == null) {
            return null;
        }
        String uriText = normalizedDomain.contains("://") ? normalizedDomain : "https://" + normalizedDomain;
        try {
            URI uri = new URI(uriText);
            if (uri.getHost() == null) {
                return null;
            }
            return hostPort(uri);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private String hostPort(URI uri) {
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return uri.getPort() >= 0 ? host + ":" + uri.getPort() : host;
    }

    private boolean isAvailablePost(ForumPost post) {
        return post != null && Integer.valueOf(1).equals(post.getStatus());
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private List<Long> convertTagIds(List<?> raw) {
        if (raw == null) {
            return null;
        }
        List<Long> result = new ArrayList<>(raw.size());
        for (Object o : raw) {
            if (o instanceof Long) {
                result.add((Long) o);
            } else if (o instanceof Number) {
                result.add(((Number) o).longValue());
            }
        }
        return result;
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
        vo.setTagIds(convertTagIds(post.getTagIds()));
        vo.setImageUrls(safeList(post.getImageUrls()));
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
