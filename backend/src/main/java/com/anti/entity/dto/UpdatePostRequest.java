package com.anti.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新帖子请求DTO
 */
@Data
public class UpdatePostRequest {

    /**
     * 帖子标题
     */
    @Size(max = 100, message = "帖子标题不能超过100个字符")
    private String title;

    /**
     * 帖子内容
     */
    @Size(max = 10000, message = "帖子内容不能超过10000个字符")
    private String content;

    /**
     * 帖子类型
     */
    @Size(max = 30, message = "帖子类型不能超过30个字符")
    @Pattern(regexp = "^$|experience|question|discussion", message = "帖子类型只能是experience、question或discussion")
    private String postType;

    /**
     * 标签ID数组
     */
    private List<Long> tagIds;

    /**
     * 图片URL数组；null表示不变，空数组表示清空
     */
    @Size(max = 9, message = "帖子图片最多9张")
    private List<@NotBlank(message = "图片URL不能为空") @Size(max = 500, message = "图片URL不能超过500个字符") String> imageUrls;

    /**
     * 状态:0禁用1正常
     */
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;

    /**
     * 是否精选:0否1是
     */
    @Min(value = 0, message = "是否精选只能为0或1")
    @Max(value = 1, message = "是否精选只能为0或1")
    private Integer isFeatured;

    /**
     * 是否置顶:0否1是
     */
    @Min(value = 0, message = "是否置顶只能为0或1")
    @Max(value = 1, message = "是否置顶只能为0或1")
    private Integer isTop;
}
