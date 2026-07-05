package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.Challenge;
import com.anti.entity.UserChallengeRecord;
import com.anti.entity.dto.CreateChallengeRequest;
import com.anti.entity.dto.SubmitChallengeRequest;
import com.anti.entity.dto.UpdateChallengeRequest;
import com.anti.entity.vo.ChallengeOverviewVO;
import com.anti.entity.vo.ChallengeProgressVO;
import com.anti.entity.vo.ChallengeRecordVO;
import com.anti.entity.vo.ChallengeResultVO;
import com.anti.entity.vo.ChallengeStatsVO;
import com.anti.entity.vo.ChallengeVO;
import com.anti.mapper.ChallengeMapper;
import com.anti.mapper.ChallengeRecordMapper;
import com.anti.mapper.UserChallengeRecordMapper;
import com.anti.service.AchievementService;
import com.anti.service.ChallengeService;
import com.anti.service.LeaderboardService;
import com.anti.service.ProfileService;
import com.anti.service.ScoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 闯关服务实现类
 */
@Slf4j
@Service
public class ChallengeServiceImpl implements ChallengeService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String TYPE_QUIZ = "quiz";
    private static final String TYPE_SCENARIO = "scenario";
    private static final String TYPE_AGENT_SCENARIO = "agent_scenario";
    private static final int AGENT_PASSING_SCORE = 75;
    private static final int AGENT_SCORE_REWARD = 100;

    @Autowired
    private ChallengeMapper challengeMapper;

    @Autowired
    private UserChallengeRecordMapper recordMapper;

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<ChallengeVO> getChallengeList(Long userId) {
        List<Challenge> challenges = safeList(challengeMapper.selectEnabledChallenges());
        List<Long> passedIds = userId != null ? safeList(recordMapper.selectPassedChallengeIds(userId)) : Collections.emptyList();

        Map<Long, Integer> highestScores = new HashMap<>();
        if (userId != null) {
            for (Long passedId : passedIds) {
                if (passedId != null) {
                    highestScores.put(passedId, recordMapper.selectHighestScore(userId, passedId));
                }
            }
        }

        List<ChallengeVO> result = new ArrayList<>();
        Set<Long> passedSet = new HashSet<>(passedIds);
        int firstLevel = challenges.stream()
                .filter(c -> !isAgentScenario(c.getType()))
                .mapToInt(c -> c.getLevelOrder() != null ? c.getLevelOrder() : Integer.MAX_VALUE)
                .min()
                .orElse(1);
        int maxPassedLevel = getMaxPassedLevel(userId);

        for (Challenge c : challenges) {
            ChallengeVO vo = convertToVO(c);
            vo.setPassed(passedSet.contains(c.getId()));
            vo.setHighestScore(highestScores.get(c.getId()));

            int levelOrder = c.getLevelOrder() != null ? c.getLevelOrder() : firstLevel;
            boolean shouldUnlock = isAgentScenario(c.getType())
                    || vo.getPassed()
                    || levelOrder <= (maxPassedLevel > 0 ? maxPassedLevel + 1 : firstLevel);
            vo.setLocked(!shouldUnlock && !vo.getPassed());

            if (vo.getLocked()) {
                vo.setUnlockHint("需要先通关第 " + (c.getLevelOrder() - 1) + " 关");
            }

            result.add(vo);
        }

        return result;
    }

    @Override
    public ChallengeVO getChallengeDetail(Long challengeId, Long userId) {
        Challenge challenge = challengeMapper.selectById(challengeId);
        if (challenge == null) {
            throw new BusinessException("关卡不存在");
        }
        if (!Objects.equals(challenge.getStatus(), 1)) {
            throw new BusinessException("关卡不存在或已禁用");
        }

        ChallengeVO vo = convertToVO(challenge);

        if (userId != null) {
            UserChallengeRecord record = recordMapper.selectLatestByUserAndChallenge(userId, challengeId);
            if (record != null) {
                vo.setPassed(record.getPassed() == 1);
                vo.setHighestScore(record.getScore());
            }

            List<Long> passedIds = safeList(recordMapper.selectPassedChallengeIds(userId));
            int levelOrder = challenge.getLevelOrder() != null ? challenge.getLevelOrder() : getFirstEnabledLevel();
            int maxPassedLevel = getMaxPassedLevel(userId);
            boolean alreadyPassed = Boolean.TRUE.equals(vo.getPassed());
            boolean shouldUnlock = isAgentScenario(challenge.getType())
                    || alreadyPassed
                    || levelOrder <= (maxPassedLevel > 0 ? maxPassedLevel + 1 : getFirstEnabledLevel());
            vo.setLocked(!shouldUnlock && !alreadyPassed);
        }

        return vo;
    }

    @Override
    public IPage<ChallengeRecordVO> getChallengeRecords(Long userId, int pageNum, int pageSize) {
        Page<UserChallengeRecord> page = new Page<>(Math.max(1, pageNum), Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE)));
        LambdaQueryWrapper<UserChallengeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserChallengeRecord::getUserId, userId);
        wrapper.orderByDesc(UserChallengeRecord::getEndTime);
        IPage<UserChallengeRecord> recordPage = recordMapper.selectPage(page, wrapper);

        Map<Long, String> challengeNames = getChallengeNames();

        return recordPage.convert(record -> {
            ChallengeRecordVO vo = new ChallengeRecordVO();
            vo.setId(record.getId());
            vo.setChallengeId(record.getChallengeId());
            vo.setChallengeTitle(challengeNames.getOrDefault(record.getChallengeId(), "未知关卡"));
            vo.setAttempts(record.getAttempts());
            vo.setScore(record.getScore());
            vo.setPassed(record.getPassed() == 1);
            vo.setAnswerDetail(record.getAnswerDetail());
            vo.setStartTime(record.getStartTime());
            vo.setEndTime(record.getEndTime());
            return vo;
        });
    }

    @Override
    @Transactional
    public ChallengeResultVO submitChallenge(SubmitChallengeRequest request, Long userId) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        Challenge challenge = challengeMapper.selectById(request.getChallengeId());
        if (challenge == null) {
            throw new BusinessException("关卡不存在");
        }
        if (!Objects.equals(challenge.getStatus(), 1)) {
            throw new BusinessException("关卡不存在或已禁用");
        }

        if (!TYPE_QUIZ.equals(challenge.getType())) {
            throw new BusinessException("该关卡不是答题类型");
        }

        ChallengeVO vo = getChallengeDetail(request.getChallengeId(), userId);
        if (vo.getLocked() != null && vo.getLocked()) {
            throw new BusinessException("关卡未解锁");
        }

        Challenge.ChallengeContent content = challenge.getContent();
        if (content == null || content.getQuestions() == null) {
            throw new BusinessException("关卡题目数据异常");
        }
        validateQuizContent(content);

        List<Challenge.ChallengeContent.Question> questions = content.getQuestions();
        Map<String, List<Integer>> userAnswers = request.getAnswers();
        validateSubmittedAnswers(questions, userAnswers);

        int totalScore = 0;
        int maxScore = 0;
        int correctCount = 0;
        List<UserChallengeRecord.AnswerDetail.QuestionAnswer> answers = new ArrayList<>();

        for (Challenge.ChallengeContent.Question q : questions) {
            maxScore += q.getScore() != null ? q.getScore() : 10;

            UserChallengeRecord.AnswerDetail.QuestionAnswer qa = new UserChallengeRecord.AnswerDetail.QuestionAnswer();
            qa.setQuestionId(q.getId());
            qa.setSelectedIndexes(userAnswers.getOrDefault(q.getId(), Collections.emptyList()));
            qa.setCorrectIndexes(q.getCorrectIndexes());

            List<Integer> userSelect = userAnswers.getOrDefault(q.getId(), Collections.emptyList());
            List<Integer> correct = q.getCorrectIndexes();

            boolean isCorrect = correct != null && new HashSet<>(correct).equals(new HashSet<>(userSelect));
            qa.setCorrect(isCorrect);

            int questionScore = 0;
            if (isCorrect) {
                correctCount++;
                questionScore = q.getScore() != null ? q.getScore() : 10;
            }
            qa.setScore(questionScore);
            totalScore += questionScore;

            answers.add(qa);
        }

        boolean passed = totalScore >= (challenge.getPassingScore() != null ? challenge.getPassingScore() : 60);

        UserChallengeRecord.AnswerDetail detail = new UserChallengeRecord.AnswerDetail();
        detail.setAnswers(answers);
        detail.setTotalScore(totalScore);
        detail.setMaxScore(maxScore);
        detail.setCorrectCount(correctCount);

        LocalDateTime submittedStartTime = resolveSubmitStartTime(request.getStartTime());
        UserChallengeRecord record = recordMapper.selectLatestByUserAndChallenge(userId, request.getChallengeId());
        if (record != null && request.getStartTime() != null && submittedStartTime.equals(record.getStartTime())) {
            throw new BusinessException("请勿重复提交同一次答题");
        }
        boolean alreadyPassed = safeList(recordMapper.selectPassedChallengeIds(userId)).contains(request.getChallengeId());
        int attempts = 1;
        if (record != null) {
            attempts = record.getAttempts() + 1;
        }

        UserChallengeRecord newRecord = new UserChallengeRecord();
        newRecord.setUserId(userId);
        newRecord.setChallengeId(request.getChallengeId());
        newRecord.setAttempts(attempts);
        newRecord.setScore(totalScore);
        newRecord.setPassed(passed ? 1 : 0);
        newRecord.setAnswerDetail(detail);
        newRecord.setStartTime(submittedStartTime);
        newRecord.setEndTime(LocalDateTime.now());
        try {
            recordMapper.insert(newRecord);
        } catch (Exception e) {
            // 数据库/JSON 序列化失败时，转为业务异常避免 500
            log.warn("保存闯关记录失败 userId={} challengeId={} err={}",
                    userId, request.getChallengeId(), e.getClass().getSimpleName());
            throw new BusinessException("保存闯关记录失败，请稍后重试");
        }

        try {
            achievementService.refreshContinuousLearningStreak(userId);
        } catch (Exception e) {
            log.warn("闯关提交后连续学习成就校验失败 userId={} msg={}", userId, e.getMessage());
        }

        ChallengeResultVO result = new ChallengeResultVO();
        result.setPassed(passed);
        result.setScore(totalScore);
        result.setMaxScore(maxScore);
        result.setCorrectCount(correctCount);
        result.setTotalCount(questions.size());
        result.setAnswerDetail(detail);

        Integer highestScoreValue = recordMapper.selectHighestScore(userId, request.getChallengeId());
        int highestScore = highestScoreValue != null ? highestScoreValue : 0;
        result.setNewRecord(totalScore > highestScore);
        result.setHighestScore(Math.max(totalScore, highestScore));

        if (passed) {
            int earnedScore = challenge.getScoreReward() != null ? challenge.getScoreReward() : 0;
            result.setEarnedScore(alreadyPassed ? 0 : earnedScore);

            if (!alreadyPassed && earnedScore > 0) {
                try {
                    scoreService.addScore(userId, earnedScore, "闯关奖励");
                    leaderboardService.updateScore(userId, earnedScore, "daily");
                    leaderboardService.updateScore(userId, earnedScore, "weekly");
                    leaderboardService.updateScore(userId, earnedScore, "all");
                } catch (Exception e) {
                    log.warn("闯关奖励发放失败 userId={} challengeId={} err={}",
                            userId, challenge.getId(), e.getClass().getSimpleName());
                    throw new BusinessException("积分或排行榜更新失败，请稍后重试");
                }
            }

            try {
                int passedCount = recordMapper.countPassedChallenges(userId);
                achievementService.checkAndUnlockAchievements(userId, "challenge_count", passedCount);
                if (maxScore > 0 && totalScore >= maxScore) {
                    achievementService.checkAndUnlockAchievements(userId, "perfect_score", 1);
                }
                long totalEnabled = challengeMapper.selectCount(
                        new LambdaQueryWrapper<Challenge>().eq(Challenge::getStatus, 1));
                if (totalEnabled > 0 && passedCount >= totalEnabled) {
                    achievementService.checkAndUnlockAchievements(userId, "challenge_complete", 1);
                }
            } catch (Exception e) {
                log.warn("闯关通关后成就校验失败 userId={} msg={}", userId, e.getMessage());
            }

            try {
                int difficulty = challenge.getDifficulty() != null ? challenge.getDifficulty() : 1;
                int knowledgeGain = Math.round((totalScore * difficulty) / 20f);
                if (knowledgeGain > 0) {
                    var profile = profileService.getProfileByUserId(userId);
                    if (profile != null) {
                        int newLevel = Math.min(100, (profile.getKnowledgeLevel() != null ? profile.getKnowledgeLevel() : 0) + knowledgeGain);
                        profileService.updateKnowledgeLevel(userId, newLevel);
                        log.info("闯关通关更新知识水平 userId={} gain={} newLevel={}", userId, knowledgeGain, newLevel);
                    }
                }
            } catch (Exception e) {
                log.warn("闯关通关后更新知识水平失败 userId={} msg={}", userId, e.getMessage());
            }
        }

        String rating = ChallengeResultVO.getRating(totalScore, maxScore);
        result.setRating(rating);
        result.setRatingDesc(ChallengeResultVO.getRatingDesc(rating));

        return result;
    }

    @Override
    public ChallengeVO createChallenge(CreateChallengeRequest request) {
        validateChallengeType(request.getType());
        validateChallengeDefinition(request.getType(), request.getContent(), request.getScripts(), request.getAgentConfig());

        Challenge challenge = new Challenge();
        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setLevelOrder(request.getLevelOrder());
        challenge.setDifficulty(request.getDifficulty());
        challenge.setType(request.getType());
        challenge.setPassingScore(isAgentScenario(request.getType()) ? AGENT_PASSING_SCORE : request.getPassingScore() != null ? request.getPassingScore() : 60);
        challenge.setScoreReward(isAgentScenario(request.getType()) ? AGENT_SCORE_REWARD : request.getScoreReward() != null ? request.getScoreReward() : 10);
        challenge.setContent(request.getContent());
        challenge.setScripts(request.getScripts());
        challenge.setAgentConfig(request.getAgentConfig());
        challenge.setStatus(1);
        challenge.setCreateTime(LocalDateTime.now());
        challengeMapper.insert(challenge);
        return convertToVO(challenge);
    }

    @Override
    public ChallengeVO updateChallenge(Long challengeId, UpdateChallengeRequest request) {
        Challenge challenge = challengeMapper.selectById(challengeId);
        if (challenge == null) {
            throw new BusinessException("关卡不存在");
        }

        if (request.getTitle() != null) challenge.setTitle(request.getTitle());
        if (request.getDescription() != null) challenge.setDescription(request.getDescription());
        if (request.getLevelOrder() != null) challenge.setLevelOrder(request.getLevelOrder());
        if (request.getDifficulty() != null) challenge.setDifficulty(request.getDifficulty());
        if (request.getType() != null) challenge.setType(request.getType());
        if (request.getPassingScore() != null) challenge.setPassingScore(request.getPassingScore());
        if (request.getScoreReward() != null) challenge.setScoreReward(request.getScoreReward());
        if (request.getContent() != null) challenge.setContent(request.getContent());
        if (request.getScripts() != null) challenge.setScripts(request.getScripts());
        if (request.getAgentConfig() != null) challenge.setAgentConfig(request.getAgentConfig());
        if (request.getStatus() != null) challenge.setStatus(request.getStatus());

        validateChallengeType(challenge.getType());
        if (isAgentScenario(challenge.getType())) {
            challenge.setPassingScore(AGENT_PASSING_SCORE);
            challenge.setScoreReward(AGENT_SCORE_REWARD);
        }
        validateChallengeDefinition(challenge.getType(), challenge.getContent(), challenge.getScripts(), challenge.getAgentConfig());

        challengeMapper.updateById(challenge);
        return convertToVO(challenge);
    }

    @Override
    public void deleteChallenge(Long challengeId) {
        challengeMapper.deleteById(challengeId);
    }

    @Override
    public IPage<ChallengeVO> getAdminChallengeList(int pageNum, int pageSize, String keyword, String type, Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException("状态只能为0或1");
        }
        Page<Challenge> page = new Page<>(Math.max(1, pageNum), Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE)));
        LambdaQueryWrapper<Challenge> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Challenge::getTitle, keyword.trim());
        }
        if (type != null && !type.isBlank()) {
            validateChallengeType(type);
            wrapper.eq(Challenge::getType, type);
        }
        if (status != null) {
            wrapper.eq(Challenge::getStatus, status);
        }
        wrapper.orderByAsc(Challenge::getLevelOrder);
        IPage<Challenge> challengePage = challengeMapper.selectPage(page, wrapper);
        return challengePage.convert(this::convertToVO);
    }

    @Override
    public ChallengeProgressVO getChallengeProgress(Long userId) {
        List<Challenge> challenges = safeList(challengeMapper.selectEnabledChallenges());
        int passedCount = recordMapper.countPassedChallenges(userId);

        ChallengeProgressVO progress = new ChallengeProgressVO();
        progress.setTotalChallenges(challenges.size());
        progress.setCompletedChallenges(passedCount);

        List<ChallengeVO> nextChallenges = getChallengeList(userId).stream()
                .filter(c -> c.getLocked() != null && !c.getLocked() && !c.getPassed())
                .limit(3)
                .collect(Collectors.toList());
        progress.setNextChallenges(nextChallenges);

        return progress;
    }

    private ChallengeVO convertToVO(Challenge challenge) {
        ChallengeVO vo = new ChallengeVO();
        vo.setId(challenge.getId());
        vo.setTitle(challenge.getTitle());
        vo.setDescription(challenge.getDescription());
        vo.setLevelOrder(challenge.getLevelOrder());
        vo.setDifficulty(challenge.getDifficulty());
        vo.setDifficultyName(ChallengeVO.getDifficultyName(challenge.getDifficulty() != null ? challenge.getDifficulty() : 1));
        vo.setType(challenge.getType());
        vo.setTypeName(ChallengeVO.getTypeName(challenge.getType()));
        vo.setPassingScore(isAgentScenario(challenge.getType()) ? AGENT_PASSING_SCORE : challenge.getPassingScore());
        vo.setScoreReward(challenge.getScoreReward());
        vo.setContent(challenge.getContent());
        vo.setScripts(challenge.getScripts());
        vo.setAgentConfig(challenge.getAgentConfig());
        vo.setStatus(challenge.getStatus());
        vo.setCreateTime(challenge.getCreateTime());
        return vo;
    }

    private Map<Long, String> getChallengeNames() {
        List<Challenge> challenges = safeList(challengeMapper.selectEnabledChallenges());
        Map<Long, String> names = new HashMap<>();
        for (Challenge c : challenges) {
            names.put(c.getId(), c.getTitle());
        }
        return names;
    }

    private int getMaxPassedLevel(Long userId) {
        if (userId == null) return 0;
        Integer maxLevel = challengeMapper.selectMaxPassedNonAgentLevel(userId);
        return maxLevel != null ? maxLevel : 0;
    }

    private int getFirstEnabledLevel() {
        return safeList(challengeMapper.selectEnabledChallenges()).stream()
                .filter(c -> !isAgentScenario(c.getType()))
                .mapToInt(c -> c.getLevelOrder() != null ? c.getLevelOrder() : Integer.MAX_VALUE)
                .min()
                .orElse(1);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private boolean isAgentScenario(String type) {
        return TYPE_AGENT_SCENARIO.equals(type);
    }

    private LocalDateTime resolveSubmitStartTime(Long startTime) {
        if (startTime == null) {
            return LocalDateTime.now().minusMinutes(10);
        }
        try {
            return LocalDateTime.ofEpochSecond(startTime / 1000, 0, java.time.ZoneOffset.ofHours(8));
        } catch (Exception e) {
            throw new BusinessException("开始时间格式不正确");
        }
    }

    private void validateChallengeType(String type) {
        if (!TYPE_QUIZ.equals(type) && !TYPE_SCENARIO.equals(type) && !TYPE_AGENT_SCENARIO.equals(type)) {
            throw new BusinessException("关卡类型只能是quiz、scenario或agent_scenario");
        }
    }

    private void validateChallengeDefinition(String type,
                                             Challenge.ChallengeContent content,
                                             Challenge.ScenarioScript scripts,
                                             Challenge.AgentConfig agentConfig) {
        if (TYPE_QUIZ.equals(type)) {
            validateQuizContent(content);
            return;
        }
        if (TYPE_AGENT_SCENARIO.equals(type)) {
            validateAgentConfig(agentConfig);
            return;
        }
        validateScenarioScript(scripts);
    }

    private void validateAgentConfig(Challenge.AgentConfig config) {
        if (config == null) {
            throw new BusinessException("Agent模拟挑战必须配置agentConfig");
        }
        if (config.getFraudType() == null || config.getFraudType().isBlank()) {
            throw new BusinessException("Agent模拟挑战诈骗类型不能为空");
        }
        if (config.getScenarioBrief() == null || config.getScenarioBrief().isBlank()) {
            throw new BusinessException("Agent模拟挑战场景简介不能为空");
        }
        if (config.getPersona() == null || config.getPersona().isBlank()) {
            throw new BusinessException("Agent模拟挑战Agent身份不能为空");
        }
        if (config.getRiskPoints() == null || config.getRiskPoints().isEmpty()) {
            throw new BusinessException("Agent模拟挑战至少需要1个核心风险点");
        }
        if (config.getSafeActions() == null || config.getSafeActions().isEmpty()) {
            throw new BusinessException("Agent模拟挑战至少需要1个安全应对点");
        }
    }

    private void validateQuizContent(Challenge.ChallengeContent content) {
        if (content == null || content.getQuestions() == null || content.getQuestions().isEmpty()) {
            throw new BusinessException("答题关卡至少需要1道题目");
        }

        Set<String> questionIds = new HashSet<>();
        for (Challenge.ChallengeContent.Question question : content.getQuestions()) {
            if (question == null || question.getId() == null || question.getId().isBlank()) {
                throw new BusinessException("题目ID不能为空");
            }
            if (!questionIds.add(question.getId())) {
                throw new BusinessException("题目ID不能重复：" + question.getId());
            }
            if (question.getText() == null || question.getText().isBlank()) {
                throw new BusinessException("题目内容不能为空");
            }
            if (!Set.of("single", "multiple", "truefalse").contains(question.getQuestionType())) {
                throw new BusinessException("题目类型只能是single、multiple或truefalse");
            }
            if (question.getScore() == null || question.getScore() <= 0 || question.getScore() > 100) {
                throw new BusinessException("题目分值必须在1到100之间");
            }
            List<Challenge.ChallengeContent.Question.Option> options = question.getOptions();
            if (options == null || options.size() < 2) {
                throw new BusinessException("题目至少需要2个选项：" + question.getId());
            }
            for (Challenge.ChallengeContent.Question.Option option : options) {
                if (option == null || option.getText() == null || option.getText().isBlank()) {
                    throw new BusinessException("题目选项内容不能为空：" + question.getId());
                }
            }
            List<Integer> correctIndexes = question.getCorrectIndexes();
            if (correctIndexes == null || correctIndexes.isEmpty()) {
                throw new BusinessException("题目正确答案不能为空：" + question.getId());
            }
            if (new HashSet<>(correctIndexes).size() != correctIndexes.size()) {
                throw new BusinessException("题目正确答案索引不能重复：" + question.getId());
            }
            if (!"multiple".equals(question.getQuestionType()) && correctIndexes.size() != 1) {
                throw new BusinessException("单选或判断题只能有1个正确答案：" + question.getId());
            }
            for (Integer index : correctIndexes) {
                if (index == null || index < 0 || index >= options.size()) {
                    throw new BusinessException("题目正确答案索引越界：" + question.getId());
                }
            }
        }
    }

    private void validateSubmittedAnswers(List<Challenge.ChallengeContent.Question> questions,
                                          Map<String, List<Integer>> userAnswers) {
        Map<String, Challenge.ChallengeContent.Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Challenge.ChallengeContent.Question::getId, q -> q));

        for (String answerId : userAnswers.keySet()) {
            if (!questionMap.containsKey(answerId)) {
                throw new BusinessException("答案包含不存在的题目：" + answerId);
            }
        }

        for (Challenge.ChallengeContent.Question question : questions) {
            List<Integer> selected = userAnswers.getOrDefault(question.getId(), Collections.emptyList());
            if (new HashSet<>(selected).size() != selected.size()) {
                throw new BusinessException("答案选项不能重复：" + question.getId());
            }
            int optionSize = question.getOptions() != null ? question.getOptions().size() : 0;
            for (Integer index : selected) {
                if (index == null || index < 0 || index >= optionSize) {
                    throw new BusinessException("答案选项索引越界：" + question.getId());
                }
            }
            if (!"multiple".equals(question.getQuestionType()) && selected.size() > 1) {
                throw new BusinessException("单选或判断题只能选择1个答案：" + question.getId());
            }
        }
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
            if (node.getTitle() == null || node.getTitle().isBlank()) {
                throw new BusinessException("节点标题不能为空：" + node.getId());
            }
            if (!Set.of("start", "dialog", "decision", "result", "end").contains(node.getType())) {
                throw new BusinessException("节点类型非法：" + node.getId());
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
            if (!isValidScenarioScoreType(edge.getScoreType())) {
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

    private boolean isValidScenarioScoreType(String scoreType) {
        if (scoreType == null || scoreType.isBlank()) {
            return true;
        }
        String normalized = scoreType.trim().toLowerCase();
        return "none".equals(normalized) || "safe".equals(normalized) || "risk".equals(normalized);
    }

    @Override
    public ChallengeOverviewVO getChallengeOverview() {
        ChallengeOverviewVO overview = new ChallengeOverviewVO();

        // 总关卡数统计
        Long totalChallenges = challengeMapper.selectCount(null);
        overview.setTotalChallenges(totalChallenges);

        Long enabledChallenges = challengeMapper.selectCount(
                new LambdaQueryWrapper<Challenge>().eq(Challenge::getStatus, 1));
        overview.setEnabledChallenges(enabledChallenges);
        overview.setDisabledChallenges(totalChallenges - enabledChallenges);

        Long quizChallenges = challengeMapper.selectCount(
                new LambdaQueryWrapper<Challenge>().eq(Challenge::getType, TYPE_QUIZ));
        Long scenarioChallenges = challengeMapper.selectCount(
                new LambdaQueryWrapper<Challenge>().eq(Challenge::getType, TYPE_SCENARIO));
        Long agentScenarioChallenges = challengeMapper.selectCount(
                new LambdaQueryWrapper<Challenge>().eq(Challenge::getType, TYPE_AGENT_SCENARIO));
        overview.setQuizChallenges(quizChallenges);
        overview.setScenarioChallenges(scenarioChallenges);
        overview.setAgentScenarioChallenges(agentScenarioChallenges);

        // 参与数据统计
        Long totalAttempts = recordMapper.countAllAttempts();
        overview.setTotalAttempts(totalAttempts);

        Long totalPassedUsers = recordMapper.countTotalPassedUsers();
        overview.setTotalPassedUsers(totalPassedUsers);

        // 整体通过率
        if (totalAttempts != null && totalAttempts > 0) {
            double passRate = (double) totalPassedUsers / totalAttempts * 100;
            overview.setOverallPassRate(Math.round(passRate * 100.0) / 100.0);
        } else {
            overview.setOverallPassRate(0.0);
        }

        // 今日通关数
        Long todayPassed = recordMapper.countTodayPassed();
        overview.setTodayPassed(todayPassed);

        // 各关卡详细统计
        List<Challenge> allChallenges = challengeMapper.selectList(
                new LambdaQueryWrapper<Challenge>().orderByAsc(Challenge::getLevelOrder));
        List<ChallengeStatsVO> challengeStats = allChallenges.stream()
                .map(c -> getChallengeStats(c.getId()))
                .collect(Collectors.toList());
        overview.setChallengeStats(challengeStats);

        return overview;
    }

    @Override
    public ChallengeStatsVO getChallengeStats(Long challengeId) {
        Challenge challenge = challengeMapper.selectById(challengeId);
        ChallengeStatsVO stats = new ChallengeStatsVO();
        stats.setChallengeId(challengeId);
        stats.setTitle(challenge != null ? challenge.getTitle() : "未知关卡");

        Long totalAttempts = recordMapper.countByChallengeId(challengeId);
        Long passedCount = recordMapper.countPassedByChallengeId(challengeId);

        stats.setTotalAttempts(totalAttempts);
        stats.setPassedCount(passedCount);

        if (totalAttempts != null && totalAttempts > 0 && passedCount != null) {
            double passRate = (double) passedCount / totalAttempts * 100;
            stats.setPassRate(Math.round(passRate * 100.0) / 100.0);
        } else {
            stats.setPassRate(0.0);
        }

        // 从记录中计算平均分、最高分、最低分
        List<UserChallengeRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<UserChallengeRecord>()
                        .eq(UserChallengeRecord::getChallengeId, challengeId));

        if (records != null && !records.isEmpty()) {
            IntSummaryStatistics scoreStats = records.stream()
                    .mapToInt(r -> r.getScore() != null ? r.getScore() : 0)
                    .summaryStatistics();

            stats.setAvgScore(Math.round(scoreStats.getAverage() * 100.0) / 100.0);
            stats.setMaxScore(scoreStats.getMax());
            stats.setMinScore(scoreStats.getMin());

            // 平均用时
            double avgDuration = records.stream()
                    .filter(r -> r.getStartTime() != null && r.getEndTime() != null)
                    .mapToLong(r -> java.time.Duration.between(r.getStartTime(), r.getEndTime()).getSeconds())
                    .average()
                    .orElse(0.0);
            stats.setAvgDuration(Math.round(avgDuration * 100.0) / 100.0);
        } else {
            stats.setAvgScore(0.0);
            stats.setMaxScore(0);
            stats.setMinScore(0);
            stats.setAvgDuration(0.0);
        }

        return stats;
    }

    @Override
    public void batchUpdateStatus(List<Long> challengeIds, Integer status) {
        if (challengeIds == null || challengeIds.isEmpty()) {
            throw new BusinessException("请选择要操作的关卡");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态只能为0或1");
        }
        for (Long id : challengeIds) {
            Challenge challenge = challengeMapper.selectById(id);
            if (challenge != null) {
                if (status == 1) {
                    validateChallengeType(challenge.getType());
                    validateChallengeDefinition(challenge.getType(), challenge.getContent(), challenge.getScripts(), challenge.getAgentConfig());
                }
                challenge.setStatus(status);
                challengeMapper.updateById(challenge);
            }
        }
    }

    @Override
    public void batchDelete(List<Long> challengeIds) {
        if (challengeIds == null || challengeIds.isEmpty()) {
            throw new BusinessException("请选择要删除的关卡");
        }
        challengeMapper.deleteBatchIds(challengeIds);
    }
}
