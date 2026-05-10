package com.anti.entity.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class CreateNewsRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String summary;

    private String coverImage;

    private Long categoryId;

    private String newsType;

    private Integer isTop;

    private Integer isMandatory;
}
