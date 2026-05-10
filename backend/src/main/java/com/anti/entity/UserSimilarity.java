package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户相似度实体类(协同过滤用)
 * 存储用户之间的相似度计算结果，用于KNN推荐
 */
@Data
@TableName("user_similarity")
public class UserSimilarity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户A ID
     */
    private Long userIdA;

    /**
     * 用户B ID
     */
    private Long userIdB;

    /**
     * 皮尔逊相关系数( Pearson Correlation Coefficient )
     * 范围：-1.0000 到 1.0000
     * 正值表示正相关，负值表示负相关
     */
    private BigDecimal similarityScore;

    /**
     * 共同标签列表( JSON数组 )
     * 存储两个用户共同感兴趣的标签
     */
    private String commonTags;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
