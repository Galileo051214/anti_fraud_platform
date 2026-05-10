package com.anti.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论视图对象
 */
@Data
public class CommentVO {

    private Long id;

    private Long postId;

    private Long userId;

    private Long parentId;

    private String content;

    private Integer likeCount;

    private Integer status;

    private String authorName;

    private String authorAvatar;

    private Boolean isLiked;

    private Boolean isAuthor;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 子评论列表
     */
    private List<CommentVO> children;
}
