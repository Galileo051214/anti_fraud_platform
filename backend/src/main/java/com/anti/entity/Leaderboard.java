package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 排行榜实体类
 */
@Data
@TableName("leaderboard")
public class Leaderboard {

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 周期类型: daily-日榜, weekly-周榜, all-总榜
     */
    private String periodType;

    /**
     * 周期内积分
     */
    private Integer score;

    /**
     * 排名
     */
    // MySQL 中 `rank` 可能被当作关键字/函数解析，使用反引号确保列名正确。
    @TableField("`rank`")
    private Integer rank;

    /**
     * 更新日期
     */
    private LocalDate updateDate;

    // leaderboard 表结构(init.sql)中没有 create_time 字段，本实体不做持久化映射。
}
