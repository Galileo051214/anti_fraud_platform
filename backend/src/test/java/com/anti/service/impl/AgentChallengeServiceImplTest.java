package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.AgentChallengeSession;
import com.anti.entity.Challenge;
import com.anti.entity.UserChallengeRecord;
import com.anti.entity.dto.AgentChallengeReplyRequest;
import com.anti.entity.vo.AgentChallengeSessionVO;
import com.anti.mapper.AgentChallengeDailyRewardMapper;
import com.anti.mapper.AgentChallengeSessionMapper;
import com.anti.mapper.ChallengeMapper;
import com.anti.mapper.UserChallengeRecordMapper;
import com.anti.service.LeaderboardService;
import com.anti.service.ScoreService;
import com.anti.util.DeepSeekClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentChallengeServiceImplTest {

    @Mock
    private AgentChallengeSessionMapper sessionMapper;
    @Mock
    private AgentChallengeDailyRewardMapper rewardMapper;
    @Mock
    private ChallengeMapper challengeMapper;
    @Mock
    private UserChallengeRecordMapper recordMapper;
    @Mock
    private ScoreService scoreService;
    @Mock
    private LeaderboardService leaderboardService;
    @Mock
    private DeepSeekClient deepSeekClient;

    private AgentChallengeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AgentChallengeServiceImpl(
                sessionMapper,
                rewardMapper,
                challengeMapper,
                recordMapper,
                scoreService,
                leaderboardService,
                deepSeekClient,
                new ObjectMapper()
        );
    }

    @Test
    void fifthReplyCompletesSessionAndGrantsFirstDailyReward() {
        AgentChallengeSession session = inProgressSession("s1", 5);
        when(sessionMapper.selectBySessionIdAndUserId("s1", 1L)).thenReturn(session);
        when(challengeMapper.selectById(99L)).thenReturn(agentChallenge());
        when(deepSeekClient.chat(anyString(), anyString(), any())).thenReturn(success(scoringJson(96)));
        when(rewardMapper.countByUserAndDate(any(), any())).thenReturn(0);
        when(recordMapper.selectLatestByUserAndChallenge(1L, 99L)).thenReturn(null);

        AgentChallengeSessionVO result = service.replyStream(reply("s1", "我拒绝转账，会报警并联系官方客服。"), 1L, null);

        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(result.getFinalScore()).isEqualTo(96);
        assertThat(result.getPassed()).isTrue();
        assertThat(result.getRewardGranted()).isTrue();
        assertThat(result.getEarnedScore()).isEqualTo(100);
        verify(scoreService).addScore(1L, 100, "Agent模拟挑战每日奖励");
        verify(leaderboardService).updateScore(1L, 100, "daily");
        verify(leaderboardService).updateScore(1L, 100, "weekly");
        verify(leaderboardService).updateScore(1L, 100, "all");
        verify(rewardMapper).insert(any());

        ArgumentCaptor<UserChallengeRecord> recordCaptor = ArgumentCaptor.forClass(UserChallengeRecord.class);
        verify(recordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getScore()).isEqualTo(96);
        assertThat(recordCaptor.getValue().getPassed()).isEqualTo(1);
    }

    @Test
    void highRiskUserReplyCapsScoreAndDoesNotReward() {
        AgentChallengeSession session = inProgressSession("s2", 5);
        when(sessionMapper.selectBySessionIdAndUserId("s2", 1L)).thenReturn(session);
        when(challengeMapper.selectById(99L)).thenReturn(agentChallenge());
        when(deepSeekClient.chat(anyString(), anyString(), any())).thenReturn(success(scoringJson(98)));

        AgentChallengeSessionVO result = service.replyStream(reply("s2", "好的，我把验证码发给你。"), 1L, null);

        assertThat(result.getFinalScore()).isEqualTo(59);
        assertThat(result.getPassed()).isFalse();
        assertThat(result.getRewardGranted()).isFalse();
        assertThat(result.getScoringReport().getHighRiskTriggered()).isTrue();
        verify(scoreService, never()).addScore(any(), any(), any());
        verify(rewardMapper, never()).insert(any());
    }

    @Test
    void repeatedDailyPassDoesNotGrantRewardAgain() {
        AgentChallengeSession session = inProgressSession("s3", 5);
        when(sessionMapper.selectBySessionIdAndUserId("s3", 1L)).thenReturn(session);
        when(challengeMapper.selectById(99L)).thenReturn(agentChallenge());
        when(deepSeekClient.chat(anyString(), anyString(), any())).thenReturn(success(scoringJson(94)));
        when(rewardMapper.countByUserAndDate(any(), any())).thenReturn(1);

        AgentChallengeSessionVO result = service.replyStream(reply("s3", "我不会转账，会走官方渠道核实。"), 1L, null);

        assertThat(result.getPassed()).isTrue();
        assertThat(result.getRewardGranted()).isFalse();
        assertThat(result.getEarnedScore()).isZero();
        verify(scoreService, never()).addScore(any(), any(), any());
        verify(rewardMapper, never()).insert(any());
    }

    @Test
    void clientSnapshotRestoresFullConversationWhenStoredHistoryIsShort() {
        AgentChallengeSession session = inProgressSession("s5", 5);
        AgentChallengeReplyRequest request = reply("s5", "第五轮我拒绝转账并报警。");
        request.setClientMessages(List.of(
                message("agent", 1, "先做任务有返利。"),
                message("user", 1, "第一轮我先核实。"),
                message("agent", 2, "名额有限，快点垫资。"),
                message("user", 2, "第二轮我不垫资。"),
                message("agent", 3, "不做就冻结收益。"),
                message("user", 3, "第三轮我拒绝。"),
                message("agent", 4, "最后一次机会。"),
                message("user", 4, "第四轮我联系官方。"),
                message("agent", 5, "转完才能解冻。"),
                message("user", 5, "第五轮我拒绝转账并报警。")
        ));
        when(sessionMapper.selectBySessionIdAndUserId("s5", 1L)).thenReturn(session);
        when(challengeMapper.selectById(99L)).thenReturn(agentChallenge());
        when(deepSeekClient.chat(anyString(), anyString(), any())).thenReturn(success(scoringJson(74)));

        AgentChallengeSessionVO result = service.replyStream(request, 1L, null);

        assertThat(result.getPassed()).isFalse();
        assertThat(result.getRewardGranted()).isFalse();
        assertThat(result.getMessages()).hasSize(10);
        assertThat(result.getMessages())
                .extracting(AgentChallengeSession.AgentMessage::getContent)
                .contains("第一轮我先核实。", "第五轮我拒绝转账并报警。");
        ArgumentCaptor<String> scoringPrompt = ArgumentCaptor.forClass(String.class);
        verify(deepSeekClient).chat(anyString(), scoringPrompt.capture(), any());
        assertThat(scoringPrompt.getValue()).contains("第一轮我先核实。", "第四轮我联系官方。");
        verify(rewardMapper, never()).insert(any());
    }

    @Test
    void scoringUnavailableStopsCompletion() {
        AgentChallengeSession session = inProgressSession("s4", 5);
        when(sessionMapper.selectBySessionIdAndUserId("s4", 1L)).thenReturn(session);
        when(challengeMapper.selectById(99L)).thenReturn(agentChallenge());
        DeepSeekClient.DeepSeekResponse failed = new DeepSeekClient.DeepSeekResponse();
        failed.setSuccess(false);
        when(deepSeekClient.chat(anyString(), anyString(), any())).thenReturn(failed);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.replyStream(reply("s4", "我拒绝。"), 1L, null));

        assertThat(exception.getMessage()).contains("AI评分服务不可用");
        verify(sessionMapper, never()).updateById(any());
        verify(scoreService, never()).addScore(any(), any(), any());
    }

    private AgentChallengeReplyRequest reply(String sessionId, String text) {
        AgentChallengeReplyRequest request = new AgentChallengeReplyRequest();
        request.setSessionId(sessionId);
        request.setMessage(text);
        return request;
    }

    private AgentChallengeSession inProgressSession(String sessionId, int round) {
        AgentChallengeSession session = new AgentChallengeSession();
        session.setId(1L);
        session.setSessionId(sessionId);
        session.setUserId(1L);
        session.setChallengeId(99L);
        session.setStatus("in_progress");
        session.setCurrentRound(round);
        session.setMessages(List.of(message("agent", 1, "先做任务有返利。")));
        session.setPassed(0);
        session.setRewardGranted(0);
        session.setStartTime(LocalDateTime.now().minusMinutes(3));
        return session;
    }

    private AgentChallengeSession.AgentMessage message(String role, int round, String content) {
        AgentChallengeSession.AgentMessage message = new AgentChallengeSession.AgentMessage();
        message.setRole(role);
        message.setRound(round);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now().toString());
        return message;
    }

    private Challenge agentChallenge() {
        Challenge challenge = new Challenge();
        challenge.setId(99L);
        challenge.setTitle("Agent模拟：刷单返利陷阱");
        challenge.setType("agent_scenario");
        challenge.setStatus(1);
        challenge.setPassingScore(75);
        challenge.setScoreReward(100);
        Challenge.AgentConfig config = new Challenge.AgentConfig();
        config.setFraudType("刷单返利");
        config.setScenarioBrief("诱导垫资刷单");
        config.setPersona("任务导师");
        config.setRiskPoints(List.of("垫资刷单", "高额返利"));
        config.setSafeActions(List.of("拒绝转账", "官方核验"));
        challenge.setAgentConfig(config);
        return challenge;
    }

    private DeepSeekClient.DeepSeekResponse success(String content) {
        DeepSeekClient.DeepSeekResponse response = new DeepSeekClient.DeepSeekResponse();
        response.setSuccess(true);
        response.setContent(content);
        return response;
    }

    private String scoringJson(int totalScore) {
        return """
                {
                  "totalScore": %d,
                  "riskIdentificationScore": 30,
                  "highRiskRejectionScore": 30,
                  "officialVerificationScore": 18,
                  "evidenceAndHelpScore": 9,
                  "communicationStabilityScore": 9,
                  "rating": "S",
                  "summary": "识别和处置都较好",
                  "keyMistakes": [],
                  "correctActions": ["拒绝转账", "官方核验"]
                }
                """.formatted(totalScore);
    }
}
