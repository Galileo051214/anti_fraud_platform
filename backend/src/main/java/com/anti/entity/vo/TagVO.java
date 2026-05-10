package com.anti.entity.vo;

import lombok.Data;

/**
 * 标签视图对象
 */
@Data
public class TagVO {

    /**
     * 标签ID
     */
    private Long id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签分类
     */
    private String category;

    /**
     * 标签描述
     */
    private String description;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 关联案例数
     */
    private Integer caseCount;
}
