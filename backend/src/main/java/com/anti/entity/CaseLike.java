package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 案例点赞实体类
 */
@Data
@TableName("case_like")
public class CaseLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 案例ID
     */
    private Long caseId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 点赞时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
