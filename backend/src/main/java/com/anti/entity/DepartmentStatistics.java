package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 院系统计数据实体类
 */
@Data
@TableName("department_statistics")
public class DepartmentStatistics {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 统计日期
     */
    private LocalDate statDate;

    /**
     * 年级
     */
    private String grade;

    /**
     * 专业
     */
    private String major;

    /**
     * 用户数
     */
    private Integer userCount;

    /**
     * 平均知识水平
     */
    private BigDecimal avgKnowledgeLevel;

    /**
     * 平均测试得分
     */
    private BigDecimal avgTestScore;

    /**
     * 学习完成率
     */
    private BigDecimal completionRate;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
