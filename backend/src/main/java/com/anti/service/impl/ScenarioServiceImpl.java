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
import java.util.*;
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
        requireUser(userId);
        Challenge challenge = challengeMapper.selectById(challengeId);
        requireAccessibleScenario(challenge, userId);

        ScenarioProgress progress = progressMapper.selectByUserAndChallenge(userId, challengeId);
        if (progress != null) {
            return buildProgressVO(progress, challenge);
        }

        Challenge.ScenarioScript script = challenge.getScripts();
        validateScenarioScript(script);

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
        requireUser(userId);
        Challenge challenge = challengeMapper.selectById(challengeId);
        requireAccessibleScenario(challenge, userId);

        ScenarioProgress progress = progressMapper.selectByUserAndChallenge(userId, challengeId);
        if (progress == null) {
            throw new BusinessException("情景模拟记录不存在");
        }

        return buildProgressVO(progress, challenge);
    }

    @Override
    @Transactional
    public ScenarioProgressVO makeDecision(ScenarioDecisionRequest request, Long userId) {
        requireUser(userId);
        Challenge challenge = challengeMapper.selectById(request.getChallengeId());
        requireAccessibleScenario(challenge, userId);

        ScenarioProgress progress = progressMapper.selectByUserAndChallenge(userId, request.getChallengeId());
        if (progress == null) {
            throw new BusinessException("情景模拟记录不存在");
        }

        if (!"in_progress".equals(progress.getStatus())) {
            throw new BusinessException("情景模拟已结束");
        }
        if (!progress.getCurrentNode().equals(request.getCurrentNode())) {
            throw new BusinessException("当前节点状态已变化，请刷新后重试");
        }

        Challenge.ScenarioScript script = challenge.getScripts();
        validateScenarioScript(script);

        List<Challenge.ScenarioScript.ScenarioEdge> edges = script.getEdges() != null ? script.getEdges() : Collections.emptyList();
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
        String scoreType = normalizeScoreType(selectedEdge);
        record.setScoreType(scoreType);
        record.setIsSafeChoice(isSafeScore(scoreType) ? Boolean.TRUE : isRiskScore(scoreType) ? Boolean.FALSE : null);
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
        int scoredChoices = 0;
        int awardedScore = 0;

        if (scenarioEnded) {
            for (ScenarioProgress.DecisionRecord r : history) {
                String recordScoreType = normalizeScoreType(r);
                if (isSafeScore(recordScoreType)) {
                    safeChoices++;
                }
                if (isScoredChoice(recordScoreType)) {
                    scoredChoices++;
                }
            }

            finalScore = scoredChoices > 0 ? (int) (100.0 * safeChoices / scoredChoices) : 100;
            progress.setFinalScore(finalScore);

            int passingScore = challenge.getPassingScore() != null ? challenge.getPassingScore() : 60;
            passed = finalScore >= passingScore;
            progress.setStatus(passed ? "completed" : "failed");

            boolean alreadyPassed = safeList(userChallengeRecordMapper.selectPassedChallengeIds(userId)).contains(challenge.getId());
            if (passed && !alreadyPassed && "scenario".equals(challenge.getType())) {
                int reward = challenge.getScoreReward() != null ? challenge.getScoreReward() : 20;
                if (reward > 0) {
                    try {
                        scoreService.addScore(userId, reward, "情景模拟奖励");
                        leaderboardService.updateScore(userId, reward, "daily");
                        leaderboardService.updateScore(userId, reward, "weekly");
                        leaderboardService.updateScore(userId, reward, "all");
                        awardedScore = reward;
                    } catch (Exception e) {
                        log.warn("情景模拟奖励发放失败 userId={} challengeId={} err={}",
                                userId, challenge.getId(), e.getClass().getSimpleName());
                        throw new BusinessException("积分或排行榜更新失败，请稍后重试");
                    }
                }
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
                        if (profile != null) {
                            int newLevel = Math.min(100, (profile.getKnowledgeLevel() != null ? profile.getKnowledgeLevel() : 0) + knowledgeGain);
                            profileService.updateKnowledgeLevel(userId, newLevel);
                            log.info("情景模拟通关更新知识水平 userId={} gain={} newLevel={}", userId, knowledgeGain, newLevel);
                        }
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

        ScenarioProgressVO vo = buildProgressVO(progress, challenge);
        if (awardedScore > 0) {
            vo.setEarnedScore(awardedScore);
        }
        return vo;
    }

    @Override
    @Transactional
    public void resetScenario(Long challengeId, Long userId) {
        requireUser(userId);
        LambdaQueryWrapper<ScenarioProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScenarioProgress::getUserId, userId)
                .eq(ScenarioProgress::getChallengeId, challengeId);
        progressMapper.delete(wrapper);
    }

    @Override
    public ScenarioProgressVO getEnding(Long challengeId, Long userId) {
        requireUser(userId);
        Challenge challenge = challengeMapper.selectById(challengeId);
        requireAccessibleScenario(challenge, userId);

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
                nodeVO.setPosition(currentNodeData.getPosition());
                vo.setCurrentNodeDetail(nodeVO);
            }

            vo.setScript(buildFlowScriptForClient(script, progress));

            if ("in_progress".equals(progress.getStatus())) {
                List<Challenge.ScenarioScript.ScenarioEdge> edges = script.getEdges() != null ? script.getEdges() : Collections.emptyList();
                List<Challenge.ScenarioScript.ScenarioEdge> outgoingEdges = edges.stream()
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

    private Challenge.ScenarioScript buildFlowScriptForClient(Challenge.ScenarioScript source, ScenarioProgress progress) {
        Challenge.ScenarioScript copy = new Challenge.ScenarioScript();
        copy.setName(source.getName());
        copy.setDescription(source.getDescription());
        copy.setStartNodeId(source.getStartNodeId());
        copy.setEndNodeIds(source.getEndNodeIds() == null ? Collections.emptyList() : new ArrayList<>(source.getEndNodeIds()));

        List<Challenge.ScenarioScript.ScenarioNode> nodes = new ArrayList<>();
        for (Challenge.ScenarioScript.ScenarioNode sourceNode : safeList(source.getNodes())) {
            Challenge.ScenarioScript.ScenarioNode node = new Challenge.ScenarioScript.ScenarioNode();
            node.setId(sourceNode.getId());
            node.setType(sourceNode.getType());
            node.setTitle(sourceNode.getTitle());
            node.setRole(sourceNode.getRole());
            node.setRiskTip(sourceNode.getRiskTip());
            node.setPosition(sourceNode.getPosition());
            node.setContent(Objects.equals(sourceNode.getId(), progress.getCurrentNode()) ? sourceNode.getContent() : "");
            nodes.add(node);
        }
        copy.setNodes(nodes);

        List<Challenge.ScenarioScript.ScenarioEdge> edges = new ArrayList<>();
        for (Challenge.ScenarioScript.ScenarioEdge sourceEdge : safeList(source.getEdges())) {
            Challenge.ScenarioScript.ScenarioEdge edge = new Challenge.ScenarioScript.ScenarioEdge();
            edge.setFrom(sourceEdge.getFrom());
            edge.setTo(sourceEdge.getTo());
            edge.setCondition(sourceEdge.getCondition());
            edge.setLabel(sourceEdge.getLabel());
            edge.setScoreType(null);
            edge.setIsSafeChoice(null);
            edges.add(edge);
        }
        copy.setEdges(edges);
        return copy;
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
    }

    private void requireAccessibleScenario(Challenge challenge, Long userId) {
        if (challenge == null) {
            throw new BusinessException("关卡不存在");
        }
        if (!Objects.equals(challenge.getStatus(), 1)) {
            throw new BusinessException("关卡不存在或已禁用");
        }
        if (!"scenario".equals(challenge.getType())) {
            throw new BusinessException("该关卡不是情景模拟类型");
        }
        if (isLocked(challenge, userId)) {
            throw new BusinessException("关卡未解锁");
        }
    }

    private boolean isLocked(Challenge challenge, Long userId) {
        List<Long> passedIds = safeList(userChallengeRecordMapper.selectPassedChallengeIds(userId));
        if (passedIds.contains(challenge.getId())) {
            return false;
        }
        List<Challenge> enabledChallenges = safeList(challengeMapper.selectEnabledChallenges());
        if (enabledChallenges.isEmpty()) {
            enabledChallenges = List.of(challenge);
        }
        int firstLevel = enabledChallenges.stream()
                .mapToInt(c -> c.getLevelOrder() != null ? c.getLevelOrder() : Integer.MAX_VALUE)
                .min()
                .orElse(1);
        int levelOrder = challenge.getLevelOrder() != null ? challenge.getLevelOrder() : firstLevel;
        int maxPassedLevel = 0;
        if (!passedIds.isEmpty()) {
            List<Challenge> passedChallenges = safeList(challengeMapper.selectBatchIds(passedIds));
            maxPassedLevel = passedChallenges.stream()
                    .mapToInt(c -> c.getLevelOrder() != null ? c.getLevelOrder() : 0)
                    .max()
                    .orElse(0);
        }
        return levelOrder > (maxPassedLevel > 0 ? maxPassedLevel + 1 : firstLevel);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private void validateScenarioScript(Challenge.ScenarioScript script) {
        if (script == null || script.getNodes() == null || script.getNodes().isEmpty()) {
            throw new BusinessException("情景模拟至少需要1个节点");
        }
        if (script.getStartNodeId() == null || script.getStartNodeId().isBlank()) {
            throw new BusinessException("情景模拟必须设置起始节点");
        }
        if (script.getEndNodeIds() == null || script.getEndNodeIds().isEmpty()) {
            throw new BusinessException("情景模拟至少需要1个结局节点");
        }

        Map<String, Challenge.ScenarioScript.ScenarioNode> nodeMap = new HashMap<>();
        for (Challenge.ScenarioScript.ScenarioNode node : script.getNodes()) {
            if (node == null || node.getId() == null || node.getId().isBlank()) {
                throw new BusinessException("节点ID不能为空");
            }
            if (nodeMap.put(node.getId(), node) != null) {
                throw new BusinessException("节点ID不能重复：" + node.getId());
            }
        }
        if (!nodeMap.containsKey(script.getStartNodeId())) {
            throw new BusinessException("起始节点不存在");
        }
        for (String endNodeId : script.getEndNodeIds()) {
            Challenge.ScenarioScript.ScenarioNode endNode = nodeMap.get(endNodeId);
            if (endNode == null) {
                throw new BusinessException("结局节点不存在：" + endNodeId);
            }
            if (!"end".equals(endNode.getType())) {
                throw new BusinessException("结局节点类型必须为end：" + endNodeId);
            }
        }

        List<Challenge.ScenarioScript.ScenarioEdge> edges = script.getEdges() != null ? script.getEdges() : Collections.emptyList();
        Map<String, List<String>> adjacency = new HashMap<>();
        for (Challenge.ScenarioScript.ScenarioEdge edge : edges) {
            if (edge == null || edge.getFrom() == null || edge.getFrom().isBlank()
                    || edge.getTo() == null || edge.getTo().isBlank()) {
                throw new BusinessException("连接的起点和终点不能为空");
            }
            if (!nodeMap.containsKey(edge.getFrom())) {
                throw new BusinessException("连接起点不存在：" + edge.getFrom());
            }
            if (!nodeMap.containsKey(edge.getTo())) {
                throw new BusinessException("连接终点不存在：" + edge.getTo());
            }
            if (edge.getLabel() == null || edge.getLabel().isBlank()) {
                throw new BusinessException("连接选项文案不能为空：" + edge.getFrom() + "->" + edge.getTo());
            }
            if (!isValidScoreType(edge.getScoreType())) {
                throw new BusinessException("连接评分类型非法：" + edge.getFrom() + "->" + edge.getTo());
            }
            adjacency.computeIfAbsent(edge.getFrom(), key -> new ArrayList<>()).add(edge.getTo());
        }

        for (Challenge.ScenarioScript.ScenarioNode node : script.getNodes()) {
            if (!script.getEndNodeIds().contains(node.getId()) && !adjacency.containsKey(node.getId())) {
                throw new BusinessException("非结局节点必须至少有一条出边：" + node.getId());
            }
        }

        Set<String> reachable = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(script.getStartNodeId());
        reachable.add(script.getStartNodeId());
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            for (String next : adjacency.getOrDefault(current, Collections.emptyList())) {
                if (reachable.add(next)) {
                    queue.addLast(next);
                }
            }
        }
        for (String nodeId : nodeMap.keySet()) {
            if (!reachable.contains(nodeId)) {
                throw new BusinessException("存在从起始节点不可达的节点：" + nodeId);
            }
        }
    }

    private String normalizeScoreType(Challenge.ScenarioScript.ScenarioEdge edge) {
        if (edge == null) {
            return "none";
        }
        String scoreType = edge.getScoreType();
        if (scoreType != null && !scoreType.isBlank()) {
            return scoreType.trim().toLowerCase();
        }
        if (Boolean.TRUE.equals(edge.getIsSafeChoice())) {
            return "safe";
        }
        if (Boolean.FALSE.equals(edge.getIsSafeChoice())) {
            return "risk";
        }
        return "none";
    }

    private String normalizeScoreType(ScenarioProgress.DecisionRecord record) {
        if (record == null) {
            return "none";
        }
        String scoreType = record.getScoreType();
        if (scoreType != null && !scoreType.isBlank()) {
            return scoreType.trim().toLowerCase();
        }
        if (Boolean.TRUE.equals(record.getIsSafeChoice())) {
            return "safe";
        }
        if (Boolean.FALSE.equals(record.getIsSafeChoice())) {
            return "risk";
        }
        return "none";
    }

    private boolean isValidScoreType(String scoreType) {
        if (scoreType == null || scoreType.isBlank()) {
            return true;
        }
        String normalized = scoreType.trim().toLowerCase();
        return "none".equals(normalized) || "safe".equals(normalized) || "risk".equals(normalized);
    }

    private boolean isScoredChoice(String scoreType) {
        return isSafeScore(scoreType) || isRiskScore(scoreType);
    }

    private boolean isSafeScore(String scoreType) {
        return "safe".equals(scoreType);
    }

    private boolean isRiskScore(String scoreType) {
        return "risk".equals(scoreType);
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
