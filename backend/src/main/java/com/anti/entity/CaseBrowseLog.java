package com.anti.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 案例浏览记录实体类
 */
@Data
@TableName("case_browse_log")
public class CaseBrowseLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 案例ID
     */
    private Long caseId;

    /**
     * 浏览时间，由数据库 DEFAULT CURRENT_TIMESTAMP 自动填充，不参与 MyBatis-Plus 自动填充。
     */
    private LocalDateTime browseTime;

    /**
     * 停留时长(秒)
     */
    private Integer stayDuration;
}
