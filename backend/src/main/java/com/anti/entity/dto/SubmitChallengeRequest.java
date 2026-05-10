package com.anti.entity.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 提交闯关答案请求DTO
 */
@Data
public class SubmitChallengeRequest {

    /**
     * 关卡ID
     */
    private Long challengeId;

    /**
     * 题目答案Map(题目ID -> 选择的选项索引列表)
     */
    private Map<String, List<Integer>> answers;

    /**
     * 开始时间戳
     */
    private Long startTime;
}
