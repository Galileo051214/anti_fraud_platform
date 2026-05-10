package com.anti.entity.dto;

import lombok.Data;

@Data
public class UpdateNewsRequest {

    private String title;

    private String content;

    private String summary;

    private String coverImage;

    private Long categoryId;

    private String newsType;

    private Integer isTop;

    private Integer isMandatory;
}
