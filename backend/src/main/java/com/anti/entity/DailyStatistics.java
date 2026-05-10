package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日统计实体类
 */
@Data
@TableName(value = "daily_statistics", autoResultMap = true)
public class DailyStatistics {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 统计日期
     */
    private LocalDate statDate;

    /**
     * 日活用户数
     */
    private Integer dailyActiveUsers;

    /**
     * 新增用户数
     */
    private Integer newUsers;

    /**
     * 页面浏览量
     */
    private Integer totalPageViews;

    /**
     * 各类型案例浏览量(JSON格式: {"刷单诈骗": 123, "杀猪盘": 456})
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String caseViews;

    /**
     * TOP案例ID数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String topCases;

    /**
     * 闯关完成数
     */
    private Integer challengeCompletions;

    /**
     * 平均测试得分
     */
    private BigDecimal avgTestScore;

    /**
     * 新增帖子数
     */
    private Integer newPosts;

    /**
     * 新增评论数
     */
    private Integer newComments;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
