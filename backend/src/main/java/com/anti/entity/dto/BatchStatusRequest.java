package com.anti.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量更新状态请求DTO
 */
@Data
public class BatchStatusRequest {

    /**
     * 关卡ID列表
     */
    private List<Long> challengeIds;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;
}
