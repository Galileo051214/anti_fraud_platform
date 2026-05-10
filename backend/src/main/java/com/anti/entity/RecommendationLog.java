package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推荐记录日志实体类
 * 记录推荐系统产生的推荐结果，用于分析推荐效果和模型优化
 */
@Data
@TableName("recommendation_log")
public class RecommendationLog implements Serializable {

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
     * 推荐内容类型
     * news - 资讯
     * case - 案例
     * challenge - 闯关
     */
    private String itemType;

    /**
     * 推荐内容ID
     */
    private Long itemId;

    /**
     * 推荐原因( JSON格式 )
     * 例如：{"reason": "similar_users", "tags": ["刷单诈骗"], "score": 0.85}
     */
    private String recommendReason;

    /**
     * 推荐得分
     * 综合算法计算得出的推荐置信度
     */
    private BigDecimal score;

    /**
     * 用户生命周期阶段
     * newbie - 新手期
     * growing - 成长期
     * mature - 成熟期
     */
    private String lifecycleStage;

    /**
     * 是否点击
     * 0 - 未点击
     * 1 - 已点击
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer clicked;

    /**
     * 推荐时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
