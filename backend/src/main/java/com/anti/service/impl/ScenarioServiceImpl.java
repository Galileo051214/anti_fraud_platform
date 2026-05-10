package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.Challenge;
import com.anti.entity.ScenarioProgress;
import com.anti.entity.UserChallengeRecord;
import com.anti.entity.dto.ScenarioDecisionRequest;
import com.anti.entity.vo.ChallengeVO;
import com.anti.entity.vo.ScenarioProgressVO;
import com.anti.mapper.ChallengeMapper;
import com.anti.mapper.ScenarioProgressMapper;
import com.anti.mapper.UserChallengeRecordMapper;
import com.anti.service.AchievementService;
import com.anti.service.LeaderboardService;
import com.anti.service.ProfileService;
import com.anti.service.ScenarioService;
import com.anti.service.ScoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 情景模拟服务实现类(FSM有限状态机)
 */
@Slf4j
@Service
public class ScenarioServiceImpl implements ScenarioService {

    @Autowired
    private ChallengeMapper challengeMapper;

    @Autowired
    private ScenarioProgressMapper progressMapper;

    @Autowired
    private UserChallengeRecordMapper userChallengeRecordMapper;

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private ProfileService profileService;

    @Override
    @Transactional
    public ScenarioProgressVO startScenario(Long challengeId, Long userId) {
        Challenge challenge = challengeMapper.selectById(challengeId);
        if (challenge == null) {
            throw new BusinessException("关卡不存在");
        }

        if (!"scenario".equals(challenge.getType())) {
            throw new BusinessException("该关卡不是情景模拟类型");
        }

        ScenarioProgress progress = progressMapper.selectByUserAndChallenge(userId, challengeId);
        if (progress != null) {
            return buildProgressVO(progress, challenge);
        }

        Challenge.ScenarioScript script = challenge.getScripts();
        if (script == null || script.getStartNodeId() == null) {
            throw new BusinessException("剧本数据异常");
        }

        progress = new ScenarioProgress();
        progress.setUserId(userId);
        progress.setChallengeId(challengeId);
        progress.setCurrentNode(script.getStartNodeId());
        progress.setDecisionHistory(new ArrayList<>());
        progress.setStatus("in_progress");
        progress.setStartTime(LocalDateTime.now());
        progress.setFinalScore(0);
        progressMapper.insert(progress);

        return buildProgressVO(progress, challenge);
    }

    @Override
    public ScenarioProgressVO getProgress(Long challengeId, Long userId) {
        Challenge challenge = challengeMapper.selectById(challengeId);
        if (challenge == null) {
            throw new BusinessException("关卡不存在");
        }

        ScenarioProgress progress = progressMapper.selectByUserAndChallenge(userId, challengeId);
        if (progress == null) {
            throw new BusinessException("情景模拟记录不存在");
        }

        return buildProgressVO(progress, challenge);
    }

    @Override
    @Transactional
    public ScenarioProgressVO makeDecision(ScenarioDecisionRequest request, Long userId) {
        Challenge challenge = challengeMapper.selectById(request.getChallengeId());
        if (challenge == null) {
            throw new BusinessException("关卡不存在");
        }

        ScenarioProgress progress = progressMapper.selectByUserAndChallenge(userId, request.getChallengeId());
        if (progress == null) {
            throw new BusinessException("情景模拟记录不存在");
        }

        if (!"in_progress".equals(progress.getStatus())) {
            throw new BusinessException("情景模拟已结束");
        }

        Challenge.ScenarioScript script = challenge.getScripts();
        if (script == null) {
            throw new BusinessException("剧本数据异常");
        }

        List<Challenge.ScenarioScript.ScenarioEdge> edges = script.getEdges();
        Challenge.ScenarioScript.ScenarioEdge selectedEdge = null;

        for (Challenge.ScenarioScript.ScenarioEdge edge : edges) {
            if (edge.getFrom().equals(request.getCurrentNode()) &&
                    (edge.getTo().equals(request.getSelectedEdgeId()) ||
                            (edge.getFrom() + "_" + edge.getTo()).equals(request.getSelectedEdgeId()))) {
                selectedEdge = edge;
                break;
            }
        }

        if (selectedEdge == null) {
            throw new BusinessException("无效的选择");
        }

        List<ScenarioProgress.DecisionRecord> history = progress.getDecisionHistory();
        if (history == null) {
            history = new ArrayList<>();
        }

        ScenarioProgress.DecisionRecord record = new ScenarioProgress.DecisionRecord();
        record.setNodeId(request.getCurrentNode());
        record.setEdgeId(selectedEdge.getFrom() + "_" + selectedEdge.getTo());
        record.setChoiceLabel(selectedEdge.getLabel());
        record.setIsSafeChoice(selectedEdge.getIsSafeChoice());
        record.setTimestamp(LocalDateTime.now().toString());
        history.add(record);

        progress.setCurrentNode(selectedEdge.getTo());
        progress.setDecisionHistory(history);

        Challenge.ScenarioScript.ScenarioNode currentNode = findNodeById(script, selectedEdge.getTo());

        boolean isEndNode = script.getEndNodeIds() != null &&
                script.getEndNodeIds().contains(selectedEdge.getTo());

        boolean scenarioEnded = isEndNode || currentNode == null || "end".equals(currentNode.getType());

        int finalScore = 0;
        boolean passed = false;
        int safeChoices = 0;

        if (scenarioEnded) {
            int totalChoices = history.size();
            for (ScenarioProgress.DecisionRecord r : history) {
                if (r.getIsSafeChoice() != null && r.getIsSafeChoice()) {
                    safeChoices++;
                }
            }

            finalScore = totalChoices > 0 ? (int) (100.0 * safeChoices / totalChoices) : 0;
            progress.setFinalScore(finalScore);

            passed = finalScore >= 60;
            progress.setStatus(passed ? "completed" : "failed");

            if (passed && "scenario".equals(challenge.getType())) {
                int reward = challenge.getScoreReward() != null ? challenge.getScoreReward() : 20;
                scoreService.addScore(userId, reward, "情景模拟奖励");
                leaderboardService.updateScore(userId, reward, "daily");
                leaderboardService.updateScore(userId, reward, "weekly");
                leaderboardService.updateScore(userId, reward, "all");
            }
        }

        progressMapper.updateById(progress);

        if (scenarioEnded) {
            if (passed && "scenario".equals(challenge.getType())) {
                try {
                    persistScenarioPassRecord(userId, challenge, progress, finalScore, safeChoices);
                    applyChallengePassAchievements(userId, finalScore);
                } catch (Exception e) {
                    log.warn("情景模拟通关成就/闯关记录写入失败 userId={} challengeId={} msg={}",
                            userId, challenge.getId(), e.getMessage());
                }
                try {
                    int difficulty = challenge.getDifficulty() != null ? challenge.getDifficulty() : 1;
                    int knowledgeGain = Math.round((finalScore * difficulty) / 25f);
                    if (knowledgeGain > 0) {
                        var profile = profileService.getProfileByUserId(userId);
                        int newLevel = Math.min(100, (profile.getKnowledgeLevel() != null ? profile.getKnowledgeLevel() : 0) + knowledgeGain);
                        profileService.updateKnowledgeLevel(userId, newLevel);
                        log.info("情景模拟通关更新知识水平 userId={} gain={} newLevel={}", userId, knowledgeGain, newLevel);
                    }
                } catch (Exception e) {
                    log.warn("情景模拟通关后更新知识水平失败 userId={} msg={}", userId, e.getMessage());
                }
            }
            try {
                achievementService.refreshContinuousLearningStreak(userId);
            } catch (Exception e) {
                log.warn("情景模拟结束后连续学习成就校验失败 userId={} msg={}", userId, e.getMessage());
            }
        }

        return buildProgressVO(progress, challenge);
    }

    @Override
    @Transactional
    public void resetScenario(Long challengeId, Long userId) {
        LambdaQueryWrapper<ScenarioProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScenarioProgress::getUserId, userId)
                .eq(ScenarioProgress::getChallengeId, challengeId);
        progressMapper.delete(wrapper);
    }

    @Override
    public ScenarioProgressVO getEnding(Long challengeId, Long userId) {
        Challenge challenge = challengeMapper.selectById(challengeId);
        if (challenge == null) {
            throw new BusinessException("关卡不存在");
        }

        ScenarioProgress progress = progressMapper.selectByUserAndChallenge(userId, challengeId);
        if (progress == null) {
            throw new BusinessException("情景模拟记录不存在");
        }

        return buildProgressVO(progress, challenge);
    }

    private ScenarioProgressVO buildProgressVO(ScenarioProgress progress, Challenge challenge) {
        ScenarioProgressVO vo = new ScenarioProgressVO();
        vo.setId(progress.getId());
        vo.setChallengeId(progress.getChallengeId());
        vo.setChallengeTitle(challenge.getTitle());
        vo.setCurrentNode(progress.getCurrentNode());
        vo.setDecisionHistory(progress.getDecisionHistory());
        vo.setStatus(progress.getStatus());
        vo.setStatusName(ScenarioProgressVO.getStatusName(progress.getStatus()));
        vo.setStartTime(progress.getStartTime());
        vo.setFinalScore(progress.getFinalScore());
        vo.setPassed("completed".equals(progress.getStatus()));

        vo.setDifficulty(challenge.getDifficulty());
        vo.setDifficultyName(ChallengeVO.getDifficultyName(challenge.getDifficulty() != null ? challenge.getDifficulty() : 1));
        vo.setPassingScore(challenge.getPassingScore());
        vo.setScoreReward(challenge.getScoreReward());

        if ("completed".equals(progress.getStatus()) && challenge.getScoreReward() != null) {
            vo.setEarnedScore(challenge.getScoreReward());
        }

        Challenge.ScenarioScript script = challenge.getScripts();
        if (script != null) {
            Challenge.ScenarioScript.ScenarioNode currentNodeData = findNodeById(script, progress.getCurrentNode());
            if (currentNodeData != null) {
                ScenarioProgressVO.ScenarioNodeVO nodeVO = new ScenarioProgressVO.ScenarioNodeVO();
                nodeVO.setId(currentNodeData.getId());
                nodeVO.setType(currentNodeData.getType());
                nodeVO.setTitle(currentNodeData.getTitle());
                nodeVO.setContent(currentNodeData.getContent());
                nodeVO.setRole(currentNodeData.getRole());
                nodeVO.setRiskTip(currentNodeData.getRiskTip());
                vo.setCurrentNodeDetail(nodeVO);
            }

            if ("in_progress".equals(progress.getStatus())) {
                List<Challenge.ScenarioScript.ScenarioEdge> outgoingEdges = script.getEdges().stream()
                        .filter(e -> e.getFrom().equals(progress.getCurrentNode()))
                        .collect(Collectors.toList());

                List<ScenarioProgressVO.ScenarioEdgeVO> choices = new ArrayList<>();
                for (Challenge.ScenarioScript.ScenarioEdge edge : outgoingEdges) {
                    ScenarioProgressVO.ScenarioEdgeVO edgeVO = new ScenarioProgressVO.ScenarioEdgeVO();
                    edgeVO.setEdgeId(edge.getFrom() + "_" + edge.getTo());
                    edgeVO.setToNode(edge.getTo());
                    edgeVO.setLabel(edge.getLabel());
                    edgeVO.setCondition(edge.getCondition());
                    choices.add(edgeVO);
                }
                vo.setAvailableChoices(choices);
            }
        }

        return vo;
    }

    private Challenge.ScenarioScript.ScenarioNode findNodeById(Challenge.ScenarioScript script, String nodeId) {
        if (script.getNodes() == null) return null;
        for (Challenge.ScenarioScript.ScenarioNode node : script.getNodes()) {
            if (node.getId().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }

    /**
     * 情景模拟通关写入闯关记录，与答题关统一参与「通关数 / 全通关 / 排行榜展示」等统计。
     */
    private void persistScenarioPassRecord(Long userId, Challenge challenge, ScenarioProgress progress,
                                           int finalScore, int safeChoices) {
        UserChallengeRecord latest = userChallengeRecordMapper.selectLatestByUserAndChallenge(userId, challenge.getId());
        int attempts = latest != null ? latest.getAttempts() + 1 : 1;
        UserChallengeRecord record = new UserChallengeRecord();
        record.setUserId(userId);
        record.setChallengeId(challenge.getId());
        record.setAttempts(attempts);
        record.setScore(finalScore);
        record.setPassed(1);
        record.setStartTime(progress.getStartTime() != null ? progress.getStartTime() : LocalDateTime.now());
        record.setEndTime(LocalDateTime.now());
        UserChallengeRecord.AnswerDetail detail = new UserChallengeRecord.AnswerDetail();
        detail.setTotalScore(finalScore);
        detail.setMaxScore(100);
        detail.setCorrectCount(safeChoices);
        record.setAnswerDetail(detail);
        userChallengeRecordMapper.insert(record);
    }

    private void applyChallengePassAchievements(Long userId, int scoreForPerfectCheck) {
        int passedCount = userChallengeRecordMapper.countPassedChallenges(userId);
        achievementService.checkAndUnlockAchievements(userId, "challenge_count", passedCount);
        if (scoreForPerfectCheck >= 100) {
            achievementService.checkAndUnlockAchievements(userId, "perfect_score", 1);
        }
        long totalEnabled = challengeMapper.selectCount(
                new LambdaQueryWrapper<Challenge>().eq(Challenge::getStatus, 1));
        if (totalEnabled > 0 && passedCount >= totalEnabled) {
            achievementService.checkAndUnlockAchievements(userId, "challenge_complete", 1);
        }
    }
}
