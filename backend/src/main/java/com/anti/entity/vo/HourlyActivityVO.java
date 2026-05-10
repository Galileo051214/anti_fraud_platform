package com.anti.entity.vo;

import lombok.Data;

/**
 * 用户活跃度VO
 */
@Data
public class HourlyActivityVO {

    /**
     * 小时(0-23)
     */
    private Integer hour;

    /**
     * 活跃次数
     */
    private Integer count;
}
