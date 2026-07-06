package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.Challenge;
import com.anti.entity.UserChallengeRecord;
import com.anti.entity.UserProfile;
import com.anti.entity.dto.SubmitChallengeRequest;
import com.anti.entity.vo.ChallengeResultVO;
import com.anti.entity.vo.ChallengeVO;
import com.anti.mapper.ChallengeMapper;
import com.anti.mapper.UserChallengeRecordMapper;
import com.anti.service.AchievementService;
import com.anti.service.LeaderboardService;
import com.anti.service.ProfileService;
import com.anti.service.ScoreService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceImplTest {

    @Mock
    private ChallengeMapper challengeMapper;
    @Mock
    private UserChallengeRecordMapper recordMapper;
    @Mock
    private ScoreService scoreService;
    @Mock
    private LeaderboardService leaderboardService;
    @Mock
    private AchievementService achievementService;
    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ChallengeServiceImpl service;

    @Test
    void getChallengeListTreatsNullPassedIdsAsEmpty() {
        Challenge challenge = quizChallenge();
        when(challengeMapper.selectEnabledChallenges()).thenReturn(List.of(challenge));
        when(recordMapper.selectPassedChallengeIds(1L)).thenReturn(null);

        List<ChallengeVO> result = service.getChallengeList(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPassed()).isFalse();
        assertThat(result.get(0).getLocked()).isFalse();
        assertThat(result.get(0).getHighestScore()).isNull();
    }

    @Test
    void agentScenarioIsAlwaysUnlockedAndDoesNotAdvanceNormalUnlock() {
        Challenge first = quizChallenge();
        first.setId(10L);
        first.setLevelOrder(1);
        Challenge third = quizChallenge();
        third.setId(30L);
        third.setTitle("第三关");
        third.setLevelOrder(3);
        Challenge agent = agentChallenge();
        agent.setId(99L);
        agent.setLevelOrder(100);
        when(challengeMapper.selectEnabledChallenges()).thenReturn(List.of(first, third, agent));
        when(recordMapper.selectPassedChallengeIds(1L)).thenReturn(List.of(99L));
        when(recordMapper.selectHighestScore(1L, 99L)).thenReturn(95);
        when(challengeMapper.selectMaxPassedNonAgentLevel(1L)).thenReturn(0);

        List<ChallengeVO> result = service.getChallengeList(1L);

        ChallengeVO firstVo = result.stream().filter(c -> c.getId().equals(10L)).findFirst().orElseThrow();
        ChallengeVO thirdVo = result.stream().filter(c -> c.getId().equals(30L)).findFirst().orElseThrow();
        ChallengeVO agentVo = result.stream().filter(c -> c.getId().equals(99L)).findFirst().orElseThrow();
        assertThat(firstVo.getLocked()).isFalse();
        assertThat(thirdVo.getLocked()).isTrue();
        assertThat(agentVo.getLocked()).isFalse();
        assertThat(agentVo.getPassed()).isTrue();
        assertThat(agentVo.getHighestScore()).isEqualTo(95);
    }

    @Test
    void createAgentScenarioForcesFixedPassingScoreAndReward() {
        com.anti.entity.dto.CreateChallengeRequest request = new com.anti.entity.dto.CreateChallengeRequest();
        request.setTitle("Agent测试");
        request.setDescription("测试");
        request.setLevelOrder(100);
        request.setDifficulty(3);
        request.setType("agent_scenario");
        request.setPassingScore(1);
        request.setScoreReward(1);
        request.setAgentConfig(agentConfig());

        service.createChallenge(request);

        ArgumentCaptor<Challenge> captor = ArgumentCaptor.forClass(Challenge.class);
        verify(challengeMapper).insert(captor.capture());
        assertThat(captor.getValue().getPassingScore()).isEqualTo(75);
        assertThat(captor.getValue().getScoreReward()).isEqualTo(100);
        assertThat(captor.getValue().getAgentConfig().getFraudType()).isEqualTo("刷单返利");
    }

    @Test
    void submitChallengeAwardsScoreOnlyOnFirstPass() {
        Challenge challenge = quizChallenge();
        UserProfile profile = new UserProfile();
        profile.setKnowledgeLevel(0);
        when(challengeMapper.selectById(10L)).thenReturn(challenge);
        when(recordMapper.selectLatestByUserAndChallenge(1L, 10L)).thenReturn(null);
        when(recordMapper.selectPassedChallengeIds(1L)).thenReturn(Collections.emptyList());
        when(recordMapper.selectHighestScore(1L, 10L)).thenReturn(null);
        when(recordMapper.countPassedChallenges(1L)).thenReturn(1);
        when(challengeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(profileService.getProfileByUserId(1L)).thenReturn(profile);

        ChallengeResultVO result = service.submitChallenge(correctSubmitRequest(), 1L);

        assertThat(result.getPassed()).isTrue();
        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getEarnedScore()).isEqualTo(20);
        verify(scoreService).addScore(1L, 20, "闯关奖励");
        verify(leaderboardService).updateScore(1L, 20, "daily");
        verify(leaderboardService).updateScore(1L, 20, "weekly");
        verify(leaderboardService).updateScore(1L, 20, "all");
    }

    @Test
    void submitChallengeDoesNotAwardRepeatedPass() {
        Challenge challenge = quizChallenge();
        UserChallengeRecord latest = new UserChallengeRecord();
        latest.setAttempts(1);
        latest.setPassed(1);
        latest.setScore(100);
        when(challengeMapper.selectById(10L)).thenReturn(challenge);
        when(recordMapper.selectLatestByUserAndChallenge(1L, 10L)).thenReturn(latest);
        when(recordMapper.selectPassedChallengeIds(1L)).thenReturn(List.of(10L));
        when(challengeMapper.selectMaxPassedNonAgentLevel(1L)).thenReturn(1);
        when(recordMapper.selectHighestScore(1L, 10L)).thenReturn(100);

        ChallengeResultVO result = service.submitChallenge(correctSubmitRequest(), 1L);

        assertThat(result.getPassed()).isTrue();
        assertThat(result.getEarnedScore()).isZero();
        verify(scoreService, never()).addScore(any(), any(), any());
        verify(leaderboardService, never()).updateScore(any(), any(Integer.class), any());
    }

    @Test
    void submitChallengeRejectsDuplicateSelectedIndexes() {
        Challenge challenge = quizChallenge();
        when(challengeMapper.selectById(10L)).thenReturn(challenge);
        when(recordMapper.selectLatestByUserAndChallenge(1L, 10L)).thenReturn(null);
        when(recordMapper.selectPassedChallengeIds(1L)).thenReturn(Collections.emptyList());

        SubmitChallengeRequest request = new SubmitChallengeRequest();
        request.setChallengeId(10L);
        request.setAnswers(Map.of("q1", List.of(0, 0)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.submitChallenge(request, 1L));

        assertThat(exception.getMessage()).contains("答案选项不能重复");
        verify(recordMapper, never()).insert(any(UserChallengeRecord.class));
    }

    @Test
    void submitChallengeRejectsDisabledChallenge() {
        Challenge challenge = quizChallenge();
        challenge.setStatus(0);
        when(challengeMapper.selectById(10L)).thenReturn(challenge);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.submitChallenge(correctSubmitRequest(), 1L));

        assertThat(exception.getMessage()).contains("已禁用");
        verify(recordMapper, never()).insert(any(UserChallengeRecord.class));
    }

    @Test
    void submitChallengeRejectsDuplicateStartTime() {
        Challenge challenge = quizChallenge();
        long startTime = 1_800_000_000_000L;
        LocalDateTime submittedStartTime = LocalDateTime.ofEpochSecond(startTime / 1000, 0, java.time.ZoneOffset.ofHours(8));
        UserChallengeRecord latest = new UserChallengeRecord();
        latest.setAttempts(1);
        latest.setPassed(0);
        latest.setScore(30);
        latest.setStartTime(submittedStartTime);
        when(challengeMapper.selectById(10L)).thenReturn(challenge);
        when(recordMapper.selectLatestByUserAndChallenge(1L, 10L)).thenReturn(latest);
        when(recordMapper.selectPassedChallengeIds(1L)).thenReturn(Collections.emptyList());

        SubmitChallengeRequest request = correctSubmitRequest();
        request.setStartTime(startTime);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.submitChallenge(request, 1L));

        assertThat(exception.getMessage()).contains("请勿重复提交");
        verify(recordMapper, never()).insert(any(UserChallengeRecord.class));
    }

    @Test
    void submitChallengeHidesPersistenceExceptionMessage() {
        Challenge challenge = quizChallenge();
        when(challengeMapper.selectById(10L)).thenReturn(challenge);
        when(recordMapper.selectLatestByUserAndChallenge(1L, 10L)).thenReturn(null);
        when(recordMapper.selectPassedChallengeIds(1L)).thenReturn(Collections.emptyList());
        when(recordMapper.insert(any(UserChallengeRecord.class))).thenThrow(new RuntimeException("sql-secret"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submitChallenge(correctSubmitRequest(), 1L));

        assertThat(exception.getMessage()).contains("保存闯关记录失败，请稍后重试");
        assertThat(exception.getMessage()).doesNotContain("sql-secret");
    }

    @Test
    void submitChallengeHidesRewardFailureMessage() {
        Challenge challenge = quizChallenge();
        when(challengeMapper.selectById(10L)).thenReturn(challenge);
        when(recordMapper.selectLatestByUserAndChallenge(1L, 10L)).thenReturn(null);
        when(recordMapper.selectPassedChallengeIds(1L)).thenReturn(Collections.emptyList());
        when(recordMapper.selectHighestScore(1L, 10L)).thenReturn(null);
        doThrow(new RuntimeException("score-secret"))
                .when(scoreService).addScore(1L, 20, "闯关奖励");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submitChallenge(correctSubmitRequest(), 1L));

        assertThat(exception.getMessage()).contains("积分或排行榜更新失败，请稍后重试");
        assertThat(exception.getMessage()).doesNotContain("score-secret");
    }

    private SubmitChallengeRequest correctSubmitRequest() {
        SubmitChallengeRequest request = new SubmitChallengeRequest();
        request.setChallengeId(10L);
        request.setAnswers(Map.of("q1", List.of(0)));
        return request;
    }

    private Challenge quizChallenge() {
        Challenge challenge = new Challenge();
        challenge.setId(10L);
        challenge.setTitle("测试答题关卡");
        challenge.setType("quiz");
        challenge.setLevelOrder(1);
        challenge.setDifficulty(1);
        challenge.setPassingScore(60);
        challenge.setScoreReward(20);
        challenge.setStatus(1);
        Challenge.ChallengeContent content = new Challenge.ChallengeContent();
        Challenge.ChallengeContent.Question question = new Challenge.ChallengeContent.Question();
        question.setId("q1");
        question.setQuestionType("single");
        question.setText("请选择安全做法");
        question.setScore(100);
        question.setCorrectIndexes(List.of(0));
        Challenge.ChallengeContent.Question.Option safe = new Challenge.ChallengeContent.Question.Option();
        safe.setLabel("A");
        safe.setText("通过官方渠道核实");
        Challenge.ChallengeContent.Question.Option unsafe = new Challenge.ChallengeContent.Question.Option();
        unsafe.setLabel("B");
        unsafe.setText("直接转账");
        question.setOptions(List.of(safe, unsafe));
        content.setQuestions(List.of(question));
        challenge.setContent(content);
        return challenge;
    }

    private Challenge agentChallenge() {
        Challenge challenge = new Challenge();
        challenge.setId(99L);
        challenge.setTitle("Agent模拟");
        challenge.setType("agent_scenario");
        challenge.setLevelOrder(100);
        challenge.setDifficulty(3);
        challenge.setPassingScore(75);
        challenge.setScoreReward(100);
        challenge.setStatus(1);
        challenge.setAgentConfig(agentConfig());
        return challenge;
    }

    private Challenge.AgentConfig agentConfig() {
        Challenge.AgentConfig config = new Challenge.AgentConfig();
        config.setFraudType("刷单返利");
        config.setScenarioBrief("诱导垫资刷单");
        config.setPersona("任务导师");
        config.setRiskPoints(List.of("要求垫资", "高额返利"));
        config.setSafeActions(List.of("拒绝转账", "报警求助"));
        return config;
    }
}
