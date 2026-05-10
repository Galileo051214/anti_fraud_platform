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
        List<Challenge> challenges = challengeMapper.selectEnabledChallenges();
        List<Long> passedIds = userId != null ? recordMapper.selectPassedChallengeIds(userId) : Collections.emptyList();

        Map<Long, Integer> highestScores = new HashMap<>();
        if (userId != null) {
            for (Long passedId : passedIds) {
                highestScores.put(passedId, recordMapper.selectHighestScore(userId, passedId));
            }
        }

        List<ChallengeVO> result = new ArrayList<>();
        Set<Long> passedSet = new HashSet<>(passedIds);
        Integer prevPassedLevel = null;

        for (Challenge c : challenges) {
            ChallengeVO vo = convertToVO(c);
            vo.setPassed(passedSet.contains(c.getId()));
            vo.setHighestScore(highestScores.get(c.getId()));

            if (vo.getPassed()) {
                prevPassedLevel = c.getLevelOrder();
            }

            boolean shouldUnlock = prevPassedLevel == null || c.getLevelOrder() <= prevPassedLevel + 1;
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

        ChallengeVO vo = convertToVO(challenge);

        if (userId != null) {
            UserChallengeRecord record = recordMapper.selectLatestByUserAndChallenge(userId, challengeId);
            if (record != null) {
                vo.setPassed(record.getPassed() == 1);
                vo.setHighestScore(record.getScore());
            }

            List<Long> passedIds = recordMapper.selectPassedChallengeIds(userId);
            boolean shouldUnlock = passedIds.isEmpty() || challenge.getLevelOrder() <= getMaxPassedLevel(passedIds) + 1;
            vo.setLocked(!shouldUnlock && !vo.getPassed());
        }

        return vo;
    }

    @Override
    public IPage<ChallengeRecordVO> getChallengeRecords(Long userId, int pageNum, int pageSize) {
        Page<UserChallengeRecord> page = new Page<>(pageNum, pageSize);
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

        if (!"quiz".equals(challenge.getType())) {
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

        List<Challenge.ChallengeContent.Question> questions = content.getQuestions();
        Map<String, List<Integer>> userAnswers = request.getAnswers();

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

        UserChallengeRecord record = recordMapper.selectLatestByUserAndChallenge(userId, request.getChallengeId());
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
        newRecord.setStartTime(request.getStartTime() != null ?
                LocalDateTime.ofEpochSecond(request.getStartTime() / 1000, 0, java.time.ZoneOffset.ofHours(8)) :
                LocalDateTime.now().minusMinutes(10));
        newRecord.setEndTime(LocalDateTime.now());
        try {
            recordMapper.insert(newRecord);
        } catch (Exception e) {
            // 数据库/JSON 序列化失败时，转为业务异常避免 500
            throw new BusinessException("保存闯关记录失败：" + e.getMessage());
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

        int highestScore = recordMapper.selectHighestScore(userId, request.getChallengeId()) != null ?
                recordMapper.selectHighestScore(userId, request.getChallengeId()) : 0;
        result.setNewRecord(totalScore > highestScore);
        result.setHighestScore(Math.max(totalScore, highestScore));

        if (passed) {
            int earnedScore = challenge.getScoreReward() != null ? challenge.getScoreReward() : 0;
            result.setEarnedScore(earnedScore);

            if (earnedScore > 0) {
                try {
                    scoreService.addScore(userId, earnedScore, "闯关奖励");
                    leaderboardService.updateScore(userId, earnedScore, "daily");
                    leaderboardService.updateScore(userId, earnedScore, "weekly");
                    leaderboardService.updateScore(userId, earnedScore, "all");
                } catch (Exception e) {
                    // 避免把下游异常直接暴露为 500
                    throw new BusinessException("积分或排行榜更新失败：" + e.getMessage());
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
                    int newLevel = Math.min(100, (profile.getKnowledgeLevel() != null ? profile.getKnowledgeLevel() : 0) + knowledgeGain);
                    profileService.updateKnowledgeLevel(userId, newLevel);
                    log.info("闯关通关更新知识水平 userId={} gain={} newLevel={}", userId, knowledgeGain, newLevel);
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
        Challenge challenge = new Challenge();
        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setLevelOrder(request.getLevelOrder());
        challenge.setDifficulty(request.getDifficulty());
        challenge.setType(request.getType());
        challenge.setPassingScore(request.getPassingScore() != null ? request.getPassingScore() : 60);
        challenge.setScoreReward(request.getScoreReward() != null ? request.getScoreReward() : 10);
        challenge.setContent(request.getContent());
        challenge.setScripts(request.getScripts());
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
        if (request.getStatus() != null) challenge.setStatus(request.getStatus());

        challengeMapper.updateById(challenge);
        return convertToVO(challenge);
    }

    @Override
    public void deleteChallenge(Long challengeId) {
        challengeMapper.deleteById(challengeId);
    }

    @Override
    public IPage<ChallengeVO> getAdminChallengeList(int pageNum, int pageSize, String keyword, String type) {
        Page<Challenge> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Challenge> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Challenge::getTitle, keyword);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Challenge::getType, type);
        }
        wrapper.orderByAsc(Challenge::getLevelOrder);
        IPage<Challenge> challengePage = challengeMapper.selectPage(page, wrapper);
        return challengePage.convert(this::convertToVO);
    }

    @Override
    public ChallengeProgressVO getChallengeProgress(Long userId) {
        List<Challenge> challenges = challengeMapper.selectEnabledChallenges();
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
        vo.setPassingScore(challenge.getPassingScore());
        vo.setScoreReward(challenge.getScoreReward());
        vo.setContent(challenge.getContent());
        vo.setScripts(challenge.getScripts());
        vo.setStatus(challenge.getStatus());
        vo.setCreateTime(challenge.getCreateTime());
        return vo;
    }

    private Map<Long, String> getChallengeNames() {
        List<Challenge> challenges = challengeMapper.selectEnabledChallenges();
        Map<Long, String> names = new HashMap<>();
        for (Challenge c : challenges) {
            names.put(c.getId(), c.getTitle());
        }
        return names;
    }

    private int getMaxPassedLevel(List<Long> passedIds) {
        if (passedIds.isEmpty()) return 0;
        List<Challenge> challenges = challengeMapper.selectBatchIds(passedIds);
        return challenges.stream()
                .mapToInt(c -> c.getLevelOrder() != null ? c.getLevelOrder() : 0)
                .max()
                .orElse(0);
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
                new LambdaQueryWrapper<Challenge>().eq(Challenge::getType, "quiz"));
        Long scenarioChallenges = challengeMapper.selectCount(
                new LambdaQueryWrapper<Challenge>().eq(Challenge::getType, "scenario"));
        overview.setQuizChallenges(quizChallenges);
        overview.setScenarioChallenges(scenarioChallenges);

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
        for (Long id : challengeIds) {
            Challenge challenge = challengeMapper.selectById(id);
            if (challenge != null) {
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
