package com.anti.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent模拟挑战会话
 */
@Data
@TableName(value = "agent_challenge_session", autoResultMap = true)
public class AgentChallengeSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private Long userId;

    private Long challengeId;

    /**
     * in_progress/completed/failed
     */
    private String status;

    private Integer currentRound;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<AgentMessage> messages;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private ScoringReport scoringReport;

    private String summary;

    private Integer finalScore;

    private Integer passed;

    private Integer rewardGranted;

    private LocalDate rewardDate;

    private LocalDateTime startTime;

    private LocalDateTime updateTime;

    @Data
    public static class AgentMessage {
        private String role;
        private Integer round;
        private String content;
        private String createTime;
    }

    @Data
    public static class ScoringReport {
        private Integer totalScore;
        private Integer riskIdentificationScore;
        private Integer highRiskRejectionScore;
        private Integer officialVerificationScore;
        private Integer evidenceAndHelpScore;
        private Integer communicationStabilityScore;
        private Boolean highRiskTriggered = false;
        private Boolean ruleCapApplied = false;
        private String rating;
        private String summary;
        private List<String> keyMistakes = new ArrayList<>();
        private List<String> correctActions = new ArrayList<>();
    }
}
