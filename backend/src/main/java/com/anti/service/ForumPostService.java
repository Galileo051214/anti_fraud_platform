package com.anti.service;

import com.anti.entity.dto.CreatePostRequest;
import com.anti.entity.dto.UpdatePostRequest;
import com.anti.entity.vo.CommentVO;
import com.anti.entity.vo.PostVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 论坛帖子服务接口
 */
public interface ForumPostService {

    /**
     * 分页查询帖子列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param postType 帖子类型
     * @param sortBy   排序方式: time-时间, like-点赞数, comment-评论数
     * @param keyword  搜索关键词
     * @param userId   当前用户ID(用于判断点赞状态)
     * @return 分页结果
     */
    IPage<PostVO> getPostPage(int pageNum, int pageSize, String postType, String sortBy, String keyword, Long userId);

    /**
     * 获取帖子详情
     *
     * @param postId 帖子ID
     * @param userId 当前用户ID
     * @return 帖子详情
     */
    PostVO getPostDetail(Long postId, Long userId);

    /**
     * 创建帖子
     *
     * @param request  创建请求
     * @param authorId 作者ID
     * @return 创建的帖子
     */
    PostVO createPost(CreatePostRequest request, Long authorId);

    /**
     * 更新帖子
     *
     * @param postId  帖子ID
     * @param request 更新请求
     * @param userId  当前用户ID
     * @return 更新后的帖子
     */
    PostVO updatePost(Long postId, UpdatePostRequest request, Long userId);

    /**
     * 删除帖子
     *
     * @param postId 帖子ID
     * @param userId 当前用户ID
     */
    void deletePost(Long postId, Long userId);

    /**
     * 点赞帖子
     *
     * @param postId 帖子ID
     * @param userId 用户ID
     */
    void likePost(Long postId, Long userId);

    /**
     * 取消点赞帖子
     *
     * @param postId 帖子ID
     * @param userId 用户ID
     */
    void unlikePost(Long postId, Long userId);

    /**
     * 置顶帖子(管理员)
     *
     * @param postId   帖子ID
     * @param isTop    是否置顶
     */
    void setTop(Long postId, int isTop);

    /**
     * 精选帖子(管理员)
     *
     * @param postId      帖子ID
     * @param isFeatured  是否精选
     */
    void setFeatured(Long postId, int isFeatured);

    /**
     * 获取帖子的评论列表(树形结构)
     *
     * @param postId 帖子ID
     * @param userId 当前用户ID
     * @return 评论列表
     */
    java.util.List<CommentVO> getPostComments(Long postId, Long userId);

    /**
     * 获取用户发布的帖子
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    IPage<PostVO> getUserPosts(Long userId, int pageNum, int pageSize);
}
