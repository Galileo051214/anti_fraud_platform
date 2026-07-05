package com.anti.service;

import com.anti.entity.dto.AgentChallengeReplyRequest;
import com.anti.entity.vo.AgentChallengeSessionVO;

/**
 * Agent模拟挑战服务
 */
public interface AgentChallengeService {

    AgentChallengeSessionVO startChallenge(Long challengeId, Long userId);

    AgentChallengeSessionVO replyStream(AgentChallengeReplyRequest request,
                                        Long userId,
                                        AgentChallengeStreamHandler handler);

    AgentChallengeSessionVO getSession(String sessionId, Long userId);

    interface AgentChallengeStreamHandler {
        default void onMetadata(AgentChallengeSessionVO metadata) {
        }

        default void onAgentDelta(String delta) {
        }

        default void onComplete(AgentChallengeSessionVO result) {
        }
    }
}
