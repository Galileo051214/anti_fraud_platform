package com.anti.entity.vo;

import lombok.Data;

/**
 * 排行榜视图对象
 */
@Data
public class LeaderboardVO {

    /**
     * 排名
     */
    private Integer rank;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 年级
     */
    private String grade;

    /**
     * 专业
     */
    private String major;

    /**
     * 积分
     */
    private Integer score;

    /**
     * 周期类型
     */
    private String periodType;

    /**
     * 是否是当前用户
     */
    private Boolean isCurrentUser;
}
