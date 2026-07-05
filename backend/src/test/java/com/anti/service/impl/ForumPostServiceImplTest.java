package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.ForumPost;
import com.anti.entity.PostLike;
import com.anti.entity.dto.CreatePostRequest;
import com.anti.entity.dto.UpdatePostRequest;
import com.anti.entity.vo.PostVO;
import com.anti.mapper.CommentLikeMapper;
import com.anti.mapper.CommentMapper;
import com.anti.mapper.ForumPostMapper;
import com.anti.mapper.PostLikeMapper;
import com.anti.mapper.UserMapper;
import com.anti.service.AchievementService;
import com.anti.service.LeaderboardService;
import com.anti.service.ScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumPostServiceImplTest {

    @Mock
    private ForumPostMapper forumPostMapper;
    @Mock
    private PostLikeMapper postLikeMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private CommentLikeMapper commentLikeMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AchievementService achievementService;
    @Mock
    private ScoreService scoreService;
    @Mock
    private LeaderboardService leaderboardService;

    private ForumPostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ForumPostServiceImpl(
                forumPostMapper,
                postLikeMapper,
                commentMapper,
                commentLikeMapper,
                userMapper,
                achievementService,
                scoreService,
                leaderboardService
        );
        ReflectionTestUtils.setField(service, "baseMapper", forumPostMapper);
    }

    @Test
    void updatePostDoesNotAllowAuthorToChangeModerationFields() {
        ForumPost existing = post();
        existing.setStatus(1);
        existing.setIsTop(0);
        existing.setIsFeatured(0);
        when(forumPostMapper.selectById(10L)).thenReturn(existing);

        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("更新后的标题");
        request.setStatus(0);
        request.setIsTop(1);
        request.setIsFeatured(1);

        service.updatePost(10L, request, 1L);

        ArgumentCaptor<ForumPost> captor = ArgumentCaptor.forClass(ForumPost.class);
        verify(forumPostMapper).updateById(captor.capture());
        ForumPost saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("更新后的标题");
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(saved.getIsTop()).isZero();
        assertThat(saved.getIsFeatured()).isZero();
    }

    @Test
    void setTopRejectsInvalidSwitchValueBeforePersistence() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.setTop(10L, 2));

        assertThat(exception.getMessage()).contains("是否置顶只能为0或1");
        verify(forumPostMapper, never()).selectById(any());
        verify(forumPostMapper, never()).updateById(any());
    }

    @Test
    void setFeaturedRejectsInvalidSwitchValueBeforePersistence() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.setFeatured(10L, -1));

        assertThat(exception.getMessage()).contains("是否精选只能为0或1");
        verify(forumPostMapper, never()).selectById(any());
        verify(forumPostMapper, never()).updateById(any());
    }

    @Test
    void getPostPageKeepsTypeFilterWhenSearchingKeyword() {
        when(forumPostMapper.searchByKeyword(any(), eq("刷单"), eq("question")))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        service.getPostPage(0, 500, "question", "time", "  刷单  ", 1L);

        verify(forumPostMapper).searchByKeyword(any(), eq("刷单"), eq("question"));
    }

    @Test
    void getPostPageRejectsInvalidPostTypeBeforeQuery() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getPostPage(1, 10, "invalid", "time", null, 1L));

        assertThat(exception.getMessage()).contains("帖子类型只能是");
        verify(forumPostMapper, never()).selectByTypeOrderByTime(any(), any());
    }

    @Test
    void createPostTrimsTitleAndContent() {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("  防骗经验  ");
        request.setContent("  不轻信转账  ");
        request.setPostType("question");
        request.setImageUrls(List.of(
                "  /uploads/images/20260704/a.png  ",
                "http://localhost:8080/uploads/images/20260704/b.png"
        ));

        PostVO result = service.createPost(request, 1L);

        ArgumentCaptor<ForumPost> captor = ArgumentCaptor.forClass(ForumPost.class);
        verify(forumPostMapper).insert(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("防骗经验");
        assertThat(captor.getValue().getContent()).isEqualTo("不轻信转账");
        assertThat(captor.getValue().getPostType()).isEqualTo("question");
        assertThat(captor.getValue().getImageUrls()).containsExactly(
                "/uploads/images/20260704/a.png",
                "http://localhost:8080/uploads/images/20260704/b.png"
        );
        assertThat(result.getImageUrls()).containsExactly(
                "/uploads/images/20260704/a.png",
                "http://localhost:8080/uploads/images/20260704/b.png"
        );
    }

    @Test
    void createPostRejectsTooManyImageUrlsBeforeInsert() {
        CreatePostRequest request = validCreateRequest();
        request.setImageUrls(IntStream.range(0, 10)
                .mapToObj(i -> "/uploads/images/20260704/" + i + ".png")
                .toList());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createPost(request, 1L));

        assertThat(exception.getMessage()).contains("帖子图片最多9张");
        verify(forumPostMapper, never()).insert(any(ForumPost.class));
    }

    @Test
    void createPostRejectsExternalImageDomainBeforeInsert() {
        CreatePostRequest request = validCreateRequest();
        request.setImageUrls(List.of("https://evil.example.com/uploads/images/a.png"));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createPost(request, 1L));

        assertThat(exception.getMessage()).contains("图片URL只能使用本站");
        verify(forumPostMapper, never()).insert(any(ForumPost.class));
    }

    @Test
    void updatePostLeavesImageUrlsUnchangedWhenRequestImageUrlsIsNull() {
        ForumPost existing = post();
        existing.setImageUrls(List.of("/uploads/images/old.png"));
        when(forumPostMapper.selectById(10L)).thenReturn(existing);

        UpdatePostRequest request = new UpdatePostRequest();
        request.setTitle("更新后的标题");

        service.updatePost(10L, request, 1L);

        ArgumentCaptor<ForumPost> captor = ArgumentCaptor.forClass(ForumPost.class);
        verify(forumPostMapper).updateById(captor.capture());
        assertThat(captor.getValue().getImageUrls()).containsExactly("/uploads/images/old.png");
    }

    @Test
    void updatePostClearsImageUrlsWhenRequestImageUrlsIsEmpty() {
        ForumPost existing = post();
        existing.setImageUrls(List.of("/uploads/images/old.png"));
        when(forumPostMapper.selectById(10L)).thenReturn(existing);

        UpdatePostRequest request = new UpdatePostRequest();
        request.setImageUrls(List.of());

        PostVO result = service.updatePost(10L, request, 1L);

        ArgumentCaptor<ForumPost> captor = ArgumentCaptor.forClass(ForumPost.class);
        verify(forumPostMapper).updateById(captor.capture());
        assertThat(captor.getValue().getImageUrls()).isEmpty();
        assertThat(result.getImageUrls()).isEmpty();
    }

    @Test
    void updatePostAllowsConfiguredUploadDomain() {
        ReflectionTestUtils.setField(service, "allowedUploadDomains", "https://cdn.example.com");
        ForumPost existing = post();
        when(forumPostMapper.selectById(10L)).thenReturn(existing);

        UpdatePostRequest request = new UpdatePostRequest();
        request.setImageUrls(List.of("https://cdn.example.com/uploads/images/a.png"));

        service.updatePost(10L, request, 1L);

        ArgumentCaptor<ForumPost> captor = ArgumentCaptor.forClass(ForumPost.class);
        verify(forumPostMapper).updateById(captor.capture());
        assertThat(captor.getValue().getImageUrls())
                .containsExactly("https://cdn.example.com/uploads/images/a.png");
    }

    @Test
    void getPostPageIncludesImageUrlsInReturnedRecords() {
        ForumPost existing = post();
        existing.setStatus(1);
        existing.setImageUrls(List.of("/uploads/images/list.png"));
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ForumPost> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10, 1);
        page.setRecords(List.of(existing));
        when(forumPostMapper.selectByTypeOrderByTime(any(), isNull())).thenReturn(page);

        var result = service.getPostPage(1, 10, null, "time", null, null);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getImageUrls()).containsExactly("/uploads/images/list.png");
    }

    @Test
    void getPostDetailIncludesImageUrls() {
        ForumPost existing = post();
        existing.setStatus(1);
        existing.setImageUrls(List.of("/uploads/images/detail.png"));
        when(forumPostMapper.selectById(10L)).thenReturn(existing);

        PostVO result = service.getPostDetail(10L, null);

        assertThat(result.getImageUrls()).containsExactly("/uploads/images/detail.png");
    }

    @Test
    void likePostRejectsDisabledPostBeforeWritingLike() {
        ForumPost disabled = post();
        disabled.setStatus(0);
        when(forumPostMapper.selectById(10L)).thenReturn(disabled);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.likePost(10L, 1L));

        assertThat(exception.getMessage()).contains("帖子不存在");
        verify(postLikeMapper, never()).insert(any(PostLike.class));
        verify(forumPostMapper, never()).updateById(any());
    }

    @Test
    void unlikePostDoesNotDecrementWhenLikeRecordDoesNotExist() {
        ForumPost existing = post();
        existing.setStatus(1);
        existing.setLikeCount(3);
        when(forumPostMapper.selectById(10L)).thenReturn(existing);
        when(postLikeMapper.delete(any())).thenReturn(0);

        service.unlikePost(10L, 1L);

        verify(forumPostMapper, never()).updateById(any());
    }

    private ForumPost post() {
        ForumPost post = new ForumPost();
        post.setId(10L);
        post.setUserId(1L);
        post.setTitle("原始标题");
        post.setContent("原始内容");
        post.setPostType("experience");
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        return post;
    }

    private CreatePostRequest validCreateRequest() {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("防骗经验");
        request.setContent("不轻信转账");
        request.setPostType("experience");
        return request;
    }
}
