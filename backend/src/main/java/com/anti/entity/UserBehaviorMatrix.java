package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户-标签行为矩阵实体类
 * 记录用户对各标签的行为得分，用于构建用户兴趣向量
 */
@Data
@TableName("user_behavior_matrix")
public class UserBehaviorMatrix implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 行为得分
     * 综合考虑浏览、点赞、评论等行为计算得出的兴趣得分
     * 范围：0.00 - 100.00
     */
    private BigDecimal behaviorScore;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
