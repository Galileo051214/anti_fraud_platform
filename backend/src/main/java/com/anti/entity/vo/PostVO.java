package com.anti.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子视图对象
 */
@Data
public class PostVO {

    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String postType;

    private String postTypeName;

    private List<Long> tagIds;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer isFeatured;

    private Integer isTop;

    private Integer status;

    private String authorName;

    private String authorAvatar;

    private Boolean isLiked;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 帖子类型描述
     */
    public String getPostTypeName() {
        return switch (postType) {
            case "experience" -> "经验分享";
            case "question" -> "提问";
            case "discussion" -> "讨论";
            default -> "未知";
        };
    }
}
