package com.anti.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class NewsVO implements Serializable {

    private Long id;

    private String title;

    private String content;

    private String summary;

    private String coverImage;

    private Long categoryId;

    private String categoryName;

    private Long authorId;

    private String authorName;

    private String newsType;

    private Integer isTop;

    private Integer isMandatory;

    private Integer viewCount;

    private Long likeCount;

    private Boolean isLiked;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
