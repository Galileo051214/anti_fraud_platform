package com.anti.entity.vo;

import com.anti.entity.AgentChallengeSession;
import com.anti.entity.Challenge;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent模拟挑战会话视图对象
 */
@Data
public class AgentChallengeSessionVO {

    private String sessionId;

    private Long challengeId;

    private String challengeTitle;

    private Challenge.AgentConfig agentConfig;

    private String status;

    private Integer currentRound;

    private Integer maxRounds;

    private List<AgentChallengeSession.AgentMessage> messages;

    private AgentChallengeSession.ScoringReport scoringReport;

    private String summary;

    private Integer finalScore;

    private Boolean passed;

    private Boolean rewardGranted;

    private Integer earnedScore;

    private LocalDate rewardDate;

    private LocalDateTime startTime;

    private LocalDateTime updateTime;
}
