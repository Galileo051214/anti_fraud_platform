package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.Challenge;
import com.anti.entity.ScenarioProgress;
import com.anti.entity.UserProfile;
import com.anti.entity.dto.ScenarioDecisionRequest;
import com.anti.entity.vo.ScenarioProgressVO;
import com.anti.mapper.ChallengeMapper;
import com.anti.mapper.ScenarioProgressMapper;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScenarioServiceImplTest {

    @Mock
    private ChallengeMapper challengeMapper;
    @Mock
    private ScenarioProgressMapper progressMapper;
    @Mock
    private UserChallengeRecordMapper userChallengeRecordMapper;
    @Mock
    private ScoreService scoreService;
    @Mock
    private LeaderboardService leaderboardService;
    @Mock
    private AchievementService achievementService;
    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ScenarioServiceImpl service;

    @Test
    void startScenarioTreatsNullPassedIdsAsEmpty() {
        Challenge challenge = scenarioChallenge();
        when(challengeMapper.selectById(7L)).thenReturn(challenge);
        when(userChallengeRecordMapper.selectPassedChallengeIds(1L)).thenReturn(null);
        when(challengeMapper.selectEnabledChallenges()).thenReturn(List.of(challenge));
        when(progressMapper.selectByUserAndChallenge(1L, 7L)).thenReturn(null);

        ScenarioProgressVO result = service.startScenario(7L, 1L);

        assertThat(result.getStatus()).isEqualTo("in_progress");
        assertThat(result.getCurrentNode()).isEqualTo("start");
        assertThat(result.getCurrentNodeDetail().getPosition().getX()).isEqualTo(80);
        assertThat(result.getScript()).isNotNull();
        assertThat(result.getScript().getNodes()).hasSize(2);
        assertThat(result.getScript().getNodes().get(0).getPosition().getX()).isEqualTo(80);
        assertThat(result.getScript().getEdges().get(0).getIsSafeChoice()).isNull();
        verify(progressMapper).insert(any(ScenarioProgress.class));
    }

    @Test
    void makeDecisionRejectsStaleCurrentNode() {
        when(challengeMapper.selectById(7L)).thenReturn(scenarioChallenge());
        when(progressMapper.selectByUserAndChallenge(1L, 7L)).thenReturn(inProgress("start"));

        ScenarioDecisionRequest request = decisionRequest("old-node", "end");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.makeDecision(request, 1L));

        assertThat(exception.getMessage()).contains("当前节点状态已变化");
        verify(progressMapper, never()).updateById(any());
        verify(scoreService, never()).addScore(any(), any(), any());
    }

    @Test
    void makeDecisionAwardsScenarioRewardOnFirstPass() {
        Challenge challenge = scenarioChallenge();
        UserProfile profile = new UserProfile();
        profile.setKnowledgeLevel(0);
        when(challengeMapper.selectById(7L)).thenReturn(challenge);
        when(progressMapper.selectByUserAndChallenge(1L, 7L)).thenReturn(inProgress("start"));
        when(userChallengeRecordMapper.selectPassedChallengeIds(1L)).thenReturn(Collections.emptyList());
        when(userChallengeRecordMapper.countPassedChallenges(1L)).thenReturn(1);
        when(challengeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(profileService.getProfileByUserId(1L)).thenReturn(profile);

        ScenarioProgressVO result = service.makeDecision(decisionRequest("start", "end"), 1L);

        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(result.getFinalScore()).isEqualTo(100);
        verify(scoreService).addScore(1L, 30, "情景模拟奖励");
        verify(leaderboardService).updateScore(1L, 30, "daily");
        verify(leaderboardService).updateScore(1L, 30, "weekly");
        verify(leaderboardService).updateScore(1L, 30, "all");

        ArgumentCaptor<ScenarioProgress> progressCaptor = ArgumentCaptor.forClass(ScenarioProgress.class);
        verify(progressMapper).updateById(progressCaptor.capture());
        assertThat(progressCaptor.getValue().getStatus()).isEqualTo("completed");
        assertThat(progressCaptor.getValue().getFinalScore()).isEqualTo(100);
    }

    @Test
    void makeDecisionIgnoresNoScoreTransitionsWhenCalculatingFinalScore() {
        Challenge challenge = scenarioChallengeWithNoScoreTransition();
        ScenarioProgress progress = inProgress("start");
        when(challengeMapper.selectById(7L)).thenReturn(challenge);
        when(progressMapper.selectByUserAndChallenge(1L, 7L)).thenReturn(progress);
        when(userChallengeRecordMapper.selectPassedChallengeIds(1L)).thenReturn(List.of(7L));
        when(userChallengeRecordMapper.countPassedChallenges(1L)).thenReturn(1);
        when(challengeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        ScenarioProgressVO firstStep = service.makeDecision(decisionRequest("start", "middle"), 1L);
        ScenarioProgressVO finalStep = service.makeDecision(decisionRequest("middle", "end"), 1L);

        assertThat(firstStep.getStatus()).isEqualTo("in_progress");
        assertThat(finalStep.getStatus()).isEqualTo("completed");
        assertThat(finalStep.getFinalScore()).isEqualTo(100);
        assertThat(finalStep.getDecisionHistory()).hasSize(2);
        assertThat(finalStep.getDecisionHistory().get(0).getScoreType()).isEqualTo("none");
        assertThat(finalStep.getDecisionHistory().get(0).getIsSafeChoice()).isNull();
        assertThat(finalStep.getDecisionHistory().get(1).getScoreType()).isEqualTo("safe");
    }

    @Test
    void makeDecisionHidesRewardFailureMessage() {
        Challenge challenge = scenarioChallenge();
        when(challengeMapper.selectById(7L)).thenReturn(challenge);
        when(progressMapper.selectByUserAndChallenge(1L, 7L)).thenReturn(inProgress("start"));
        when(userChallengeRecordMapper.selectPassedChallengeIds(1L)).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("score-secret"))
                .when(scoreService).addScore(1L, 30, "情景模拟奖励");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.makeDecision(decisionRequest("start", "end"), 1L));

        assertThat(exception.getMessage()).contains("积分或排行榜更新失败，请稍后重试");
        assertThat(exception.getMessage()).doesNotContain("score-secret");
        verify(progressMapper, never()).updateById(any());
    }

    @Test
    void startScenarioRejectsDisabledChallenge() {
        Challenge challenge = scenarioChallenge();
        challenge.setStatus(0);
        when(challengeMapper.selectById(7L)).thenReturn(challenge);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.startScenario(7L, 1L));

        assertThat(exception.getMessage()).contains("已禁用");
        verify(progressMapper, never()).insert(any());
    }

    private ScenarioDecisionRequest decisionRequest(String currentNode, String selectedEdgeId) {
        ScenarioDecisionRequest request = new ScenarioDecisionRequest();
        request.setChallengeId(7L);
        request.setCurrentNode(currentNode);
        request.setSelectedEdgeId(selectedEdgeId);
        return request;
    }

    private ScenarioProgress inProgress(String currentNode) {
        ScenarioProgress progress = new ScenarioProgress();
        progress.setId(100L);
        progress.setUserId(1L);
        progress.setChallengeId(7L);
        progress.setCurrentNode(currentNode);
        progress.setStatus("in_progress");
        progress.setStartTime(LocalDateTime.now().minusMinutes(1));
        progress.setDecisionHistory(new ArrayList<>());
        return progress;
    }

    private Challenge scenarioChallenge() {
        Challenge challenge = new Challenge();
        challenge.setId(7L);
        challenge.setTitle("测试情景模拟");
        challenge.setType("scenario");
        challenge.setDifficulty(1);
        challenge.setPassingScore(60);
        challenge.setScoreReward(30);
        challenge.setStatus(1);

        Challenge.ScenarioScript script = new Challenge.ScenarioScript();
        script.setStartNodeId("start");
        script.setEndNodeIds(List.of("end"));
        Challenge.ScenarioScript.ScenarioNode start = new Challenge.ScenarioScript.ScenarioNode();
        start.setId("start");
        start.setType("start");
        start.setTitle("开始");
        start.setContent("收到陌生转账要求");
        start.setRole("narrator");
        Challenge.ScenarioScript.ScenarioNode.Position startPosition = new Challenge.ScenarioScript.ScenarioNode.Position();
        startPosition.setX(80);
        startPosition.setY(160);
        start.setPosition(startPosition);
        Challenge.ScenarioScript.ScenarioNode end = new Challenge.ScenarioScript.ScenarioNode();
        end.setId("end");
        end.setType("end");
        end.setTitle("安全结束");
        end.setContent("已通过官方渠道核实");
        end.setRole("victim");
        Challenge.ScenarioScript.ScenarioNode.Position endPosition = new Challenge.ScenarioScript.ScenarioNode.Position();
        endPosition.setX(360);
        endPosition.setY(160);
        end.setPosition(endPosition);
        Challenge.ScenarioScript.ScenarioEdge edge = new Challenge.ScenarioScript.ScenarioEdge();
        edge.setFrom("start");
        edge.setTo("end");
        edge.setLabel("拒绝并核实");
        edge.setIsSafeChoice(true);
        edge.setScoreType("safe");
        script.setNodes(List.of(start, end));
        script.setEdges(List.of(edge));
        challenge.setScripts(script);
        return challenge;
    }

    private Challenge scenarioChallengeWithNoScoreTransition() {
        Challenge challenge = scenarioChallenge();
        Challenge.ScenarioScript script = challenge.getScripts();
        Challenge.ScenarioScript.ScenarioNode middle = new Challenge.ScenarioScript.ScenarioNode();
        middle.setId("middle");
        middle.setType("dialog");
        middle.setTitle("剧情推进");
        middle.setContent("先展示一段固定剧情");
        middle.setRole("narrator");
        Challenge.ScenarioScript.ScenarioNode.Position middlePosition = new Challenge.ScenarioScript.ScenarioNode.Position();
        middlePosition.setX(220);
        middlePosition.setY(160);
        middle.setPosition(middlePosition);

        Challenge.ScenarioScript.ScenarioEdge transition = new Challenge.ScenarioScript.ScenarioEdge();
        transition.setFrom("start");
        transition.setTo("middle");
        transition.setLabel("继续");
        transition.setScoreType("none");

        Challenge.ScenarioScript.ScenarioEdge safeChoice = new Challenge.ScenarioScript.ScenarioEdge();
        safeChoice.setFrom("middle");
        safeChoice.setTo("end");
        safeChoice.setLabel("拒绝并核实");
        safeChoice.setScoreType("safe");
        safeChoice.setIsSafeChoice(true);

        script.setNodes(List.of(script.getNodes().get(0), middle, script.getNodes().get(1)));
        script.setEdges(List.of(transition, safeChoice));
        return challenge;
    }
}
