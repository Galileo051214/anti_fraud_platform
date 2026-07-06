package com.anti.entity.dto;

import com.anti.entity.AgentChallengeSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Agent模拟挑战用户回复请求
 */
@Data
public class AgentChallengeReplyRequest {

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 1000, message = "回复内容不能超过1000个字符")
    private String message;

    /**
     * 前端本地对话快照。用于在数据库JSON历史读取异常或落后时兜底恢复完整对话。
     */
    private List<AgentChallengeSession.AgentMessage> clientMessages;
}
