package com.anti.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 案例浏览记录视图对象
 */
@Data
public class CaseBrowseVO {

    /**
     * 案例ID
     */
    private Long caseId;

    /**
     * 案例标题
     */
    private String caseTitle;

    /**
     * 案例类型
     */
    private String caseType;

    /**
     * 难度等级
     */
    private Integer difficultyLevel;

    /**
     * 浏览时间
     */
    private String browseTime;

    /**
     * 停留时长(秒)
     */
    private Integer stayDuration;

    /**
     * 停留时长描述
     */
    private String stayDurationDesc;

    /**
     * 标签列表
     */
    private List<String> tagNames;
}
