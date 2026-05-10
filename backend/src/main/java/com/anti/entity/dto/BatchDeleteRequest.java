package com.anti.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量删除请求DTO
 */
@Data
public class BatchDeleteRequest {

    /**
     * 关卡ID列表
     */
    private List<Long> challengeIds;
}
