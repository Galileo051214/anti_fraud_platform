package com.anti.service.impl;

import com.anti.entity.*;
import com.anti.entity.dto.CreateCaseRequest;
import com.anti.entity.dto.UpdateCaseRequest;
import com.anti.entity.vo.CaseBrowseVO;
import com.anti.entity.vo.CaseVO;
import com.anti.entity.vo.TagVO;
import com.anti.mapper.*;
import com.anti.service.AchievementService;
import com.anti.service.CacheRefreshService;
import com.anti.service.FraudCaseService;
import com.anti.service.LeaderboardService;
import com.anti.service.ProfileService;
import com.anti.service.ScoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 案例服务实现类
 * 集成Redis缓存优化
 */
@Slf4j
@Service
public class FraudCaseServiceImpl extends ServiceImpl<FraudCaseMapper, FraudCase> implements FraudCaseService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CaseTagMapper caseTagMapper;
    private final CaseTagRelationMapper caseTagRelationMapper;
    private final CaseLikeMapper caseLikeMapper;
    private final CaseBrowseLogMapper caseBrowseLogMapper;
    private final AchievementService achievementService;
    private final CacheRefreshService cacheRefreshService;
    private final ScoreService scoreService;
    private final LeaderboardService leaderboardService;
    private final ProfileService profileService;

    public FraudCaseServiceImpl(CaseTagMapper caseTagMapper,
                              CaseTagRelationMapper caseTagRelationMapper,
                              CaseLikeMapper caseLikeMapper,
                              CaseBrowseLogMapper caseBrowseLogMapper,
                              AchievementService achievementService,
                              @Lazy CacheRefreshService cacheRefreshService,
                              ScoreService scoreService,
                              LeaderboardService leaderboardService,
                              ProfileService profileService) {
        this.caseTagMapper = caseTagMapper;
        this.caseTagRelationMapper = caseTagRelationMapper;
        this.caseLikeMapper = caseLikeMapper;
        this.caseBrowseLogMapper = caseBrowseLogMapper;
        this.achievementService = achievementService;
        this.cacheRefreshService = cacheRefreshService;
        this.scoreService = scoreService;
        this.leaderboardService = leaderboardService;
        this.profileService = profileService;
    }

    @Override
    public IPage<CaseVO> getCasePage(int pageNum, int pageSize, Long tagId, String keyword) {
        Page<FraudCase> page = new Page<>(pageNum, pageSize);
        IPage<FraudCase> casePage;

        if (tagId != null) {
            casePage = baseMapper.selectCasesByTagId(page, tagId);
        } else if (keyword != null && !keyword.isBlank()) {
            casePage = page(page, new LambdaQueryWrapper<FraudCase>()
                    .eq(FraudCase::getStatus, 1)
                    .like(FraudCase::getTitle, keyword)
                    .orderByDesc(FraudCase::getIsFeatured)
                    .orderByDesc(FraudCase::getWilsonScore)
                    .orderByDesc(FraudCase::getPublishTime));
        } else {
            casePage = page(page, new LambdaQueryWrapper<FraudCase>()
                    .eq(FraudCase::getStatus, 1)
                    .orderByDesc(FraudCase::getIsFeatured)
                    .orderByDesc(FraudCase::getWilsonScore)
                    .orderByDesc(FraudCase::getPublishTime));
        }

        return convertToCaseVOPage(casePage);
    }

    @Override
    public CaseVO getCaseDetail(Long caseId, Long userId) {
        FraudCase caseEntity = getById(caseId);
        if (caseEntity == null) {
            // 案例不存在时返回 null，让控制层返回 code=200 + data=null，
            // 前端按“案例不存在”状态渲染，避免接口直接抛 500。
            return null;
        }
        CaseVO caseVO = convertToCaseVO(caseEntity);
        if (userId != null) {
            caseVO.setIsLiked(caseLikeMapper.existsByCaseIdAndUserId(caseId, userId));
        }
        return caseVO;
    }

    @Override
    @Transactional
    public CaseVO createCase(CreateCaseRequest request, Long authorId) {
        FraudCase fraudCase = new FraudCase();
        fraudCase.setTitle(request.getTitle());
        fraudCase.setCaseType(request.getCaseType());
        fraudCase.setContent(request.getContent());
        fraudCase.setScripts(request.getScripts());
        fraudCase.setTargetGrades(request.getTargetGrades());
        fraudCase.setTargetMajors(request.getTargetMajors());
        fraudCase.setDifficultyLevel(request.getDifficultyLevel() != null ? request.getDifficultyLevel() : 1);
        fraudCase.setRiskScore(request.getRiskScore() != null ? request.getRiskScore() : BigDecimal.ZERO);
        fraudCase.setStatus(1);
        fraudCase.setIsFeatured(0);
        fraudCase.setViewCount(0);
        fraudCase.setLikeCount(0);
        fraudCase.setLikeRate(BigDecimal.ZERO);
        fraudCase.setWilsonScore(BigDecimal.ZERO);
        fraudCase.setPublishTime(LocalDateTime.now());

        save(fraudCase);

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            saveTagRelations(fraudCase.getId(), request.getTagIds());
        }

        return convertToCaseVO(fraudCase);
    }

    @Override
    @Transactional
    public CaseVO updateCase(Long caseId, UpdateCaseRequest request) {
        FraudCase fraudCase = getById(caseId);
        if (fraudCase == null) {
            throw new RuntimeException("案例不存在");
        }

        if (request.getTitle() != null) fraudCase.setTitle(request.getTitle());
        if (request.getCaseType() != null) fraudCase.setCaseType(request.getCaseType());
        if (request.getContent() != null) fraudCase.setContent(request.getContent());
        if (request.getScripts() != null) fraudCase.setScripts(request.getScripts());
        if (request.getTargetGrades() != null) fraudCase.setTargetGrades(request.getTargetGrades());
        if (request.getTargetMajors() != null) fraudCase.setTargetMajors(request.getTargetMajors());
        if (request.getDifficultyLevel() != null) fraudCase.setDifficultyLevel(request.getDifficultyLevel());
        if (request.getRiskScore() != null) fraudCase.setRiskScore(request.getRiskScore());
        if (request.getStatus() != null) fraudCase.setStatus(request.getStatus());
        if (request.getIsFeatured() != null) fraudCase.setIsFeatured(request.getIsFeatured());

        updateById(fraudCase);

        if (request.getTagIds() != null) {
            caseTagRelationMapper.delete(new LambdaQueryWrapper<CaseTagRelation>().eq(CaseTagRelation::getCaseId, caseId));
            saveTagRelations(caseId, request.getTagIds());
        }
        
        // 异步刷新相关缓存
        cacheRefreshService.handleUpdateEvent("case", caseId);

        return convertToCaseVO(fraudCase);
    }

    @Override
    @Transactional
    public void deleteCase(Long caseId) {
        caseTagRelationMapper.delete(new LambdaQueryWrapper<CaseTagRelation>().eq(CaseTagRelation::getCaseId, caseId));
        removeById(caseId);
    }

    @Override
    @Transactional
    public void publishCase(Long caseId) {
        FraudCase fraudCase = getById(caseId);
        if (fraudCase == null) {
            throw new RuntimeException("案例不存在");
        }
        fraudCase.setStatus(1);
        fraudCase.setPublishTime(LocalDateTime.now());
        updateById(fraudCase);
        
        // 异步刷新相关缓存
        cacheRefreshService.handlePublishEvent("case", caseId);
    }

    @Override
    @Transactional
    public void setFeatured(Long caseId, int isFeatured) {
        FraudCase fraudCase = getById(caseId);
        if (fraudCase == null) {
            throw new RuntimeException("案例不存在");
        }
        fraudCase.setIsFeatured(isFeatured);
        updateById(fraudCase);
    }

    @Override
    @Transactional
    public void likeCase(Long caseId, Long userId) {
        if (userId == null) {
            // 前端需要登录后才能点赞；userId 为空时直接中止，避免数据库约束触发 500
            throw new com.anti.common.BusinessException(401, "请先登录后再点赞");
        }
        if (caseLikeMapper.existsByCaseIdAndUserId(caseId, userId)) {
            // 已点赞：接口设计为幂等，避免抛 RuntimeException 导致前端收到 500
            FraudCase fraudCase = getById(caseId);
            if (fraudCase != null) {
                int prevLikeCount = fraudCase.getLikeCount() == null ? 0 : fraudCase.getLikeCount();
                updateCaseLikeStats(caseId, fraudCase.getViewCount(), prevLikeCount);
            }
            return;
        }

        FraudCase fraudCase = getById(caseId);
        if (fraudCase == null) {
            throw new RuntimeException("案例不存在");
        }

        int prevLikeCount = fraudCase.getLikeCount() == null ? 0 : fraudCase.getLikeCount();

        CaseLike caseLike = new CaseLike();
        caseLike.setCaseId(caseId);
        caseLike.setUserId(userId);
        // 防御性兜底：即使未配置 MyBatis-Plus 自动填充，也确保 create_time 非空
        caseLike.setCreateTime(LocalDateTime.now());
        caseLikeMapper.insert(caseLike);

        // 后端点赞数显式同步 +1
        updateCaseLikeStats(caseId, fraudCase.getViewCount(), prevLikeCount + 1);
        
        // 异步刷新相关缓存
        cacheRefreshService.handleLikeEvent(userId, "case", caseId);
    }

    @Override
    @Transactional
    public void unlikeCase(Long caseId, Long userId) {
        if (userId == null) {
            // 前端取消点赞也必须登录；避免 userId 空导致数据库异常
            throw new com.anti.common.BusinessException(401, "请先登录后再取消点赞");
        }

        // 已取消点赞：幂等返回，避免 -1 重复扣减
        if (!caseLikeMapper.existsByCaseIdAndUserId(caseId, userId)) {
            return;
        }

        FraudCase fraudCase = getById(caseId);
        int prevLikeCount = (fraudCase == null || fraudCase.getLikeCount() == null) ? 0 : fraudCase.getLikeCount();

        caseLikeMapper.delete(new LambdaQueryWrapper<CaseLike>()
                .eq(CaseLike::getCaseId, caseId)
                .eq(CaseLike::getUserId, userId));

        // 后端点赞数显式同步 -1（不会低于 0）
        if (fraudCase != null) {
            updateCaseLikeStats(caseId, fraudCase.getViewCount(), Math.max(0, prevLikeCount - 1));
        }
    }

    @Override
    @Transactional
    public void browseCase(Long caseId, Long userId, int stayDuration) {
        if (caseId == null) return;

        // 浏览量本身不依赖登录，尽量保证页面展示逻辑可用
        baseMapper.incrementViewCount(caseId);

        // case_browse_log.user_id 在数据库里是 NOT NULL 的；当 token 解析不到 userId 时直接跳过写入，
        // 避免触发约束异常导致接口返回 500。
        if (userId == null) {
            log.warn("跳过写入案例浏览记录：userId 为空 (caseId={}, stayDuration={})", caseId, stayDuration);
            return;
        }

        CaseBrowseLog browseLog = new CaseBrowseLog();
        browseLog.setCaseId(caseId);
        browseLog.setUserId(userId);
        browseLog.setStayDuration(Math.max(0, stayDuration));

        int existedBefore = caseBrowseLogMapper.countByUserIdAndCaseId(userId, caseId);
        try {
            int rows = caseBrowseLogMapper.insert(browseLog);
            log.debug("写入案例浏览记录成功 (caseId={}, userId={}, rows={})", caseId, userId, rows);
            // 积分规则：首次浏览某案例 +2 积分
            if (rows > 0 && existedBefore == 0) {
                try {
                    scoreService.addScore(userId, 2, "浏览案例");
                    leaderboardService.updateScore(userId, 2, "daily");
                    leaderboardService.updateScore(userId, 2, "weekly");
                    leaderboardService.updateScore(userId, 2, "all");
                } catch (Exception e) {
                    log.warn("浏览案例积分发放失败 (caseId={}, userId={}): {}", caseId, userId, e.getMessage());
                }
            }
        } catch (Exception e) {
            // 兜底：浏览日志写入失败不应影响前端正常浏览体验
            log.warn("写入案例浏览记录失败 (caseId={}, userId={}): {}", caseId, userId, e.getMessage());
            return;
        }
        try {
            int distinctCases = caseBrowseLogMapper.countDistinctCasesByUserId(userId);
            log.debug("用户 {} 当前浏览不同案例数: {}", userId, distinctCases);
            achievementService.checkAndUnlockAchievements(userId, "browse_count", distinctCases);
            achievementService.refreshContinuousLearningStreak(userId);
        } catch (Exception e) {
            log.warn("案例浏览成就校验失败 (caseId={}, userId={}): {}", caseId, userId, e.getMessage(), e);
        }
        try {
            int knowledgeGain = Math.max(1, stayDuration / 30);
            knowledgeGain = Math.min(knowledgeGain, 5);
            var profile = profileService.getProfileByUserId(userId);
            int newLevel = Math.min(100, (profile.getKnowledgeLevel() != null ? profile.getKnowledgeLevel() : 0) + knowledgeGain);
            profileService.updateKnowledgeLevel(userId, newLevel);
            log.debug("浏览案例更新知识水平 userId={} gain={} newLevel={}", userId, knowledgeGain, newLevel);
        } catch (Exception e) {
            log.warn("浏览案例更新知识水平失败 userId={} msg={}", userId, e.getMessage());
        }
    }

    @Override
    public IPage<CaseBrowseVO> getBrowseHistory(Long userId, int pageNum, int pageSize) {
        Page<CaseBrowseLog> page = new Page<>(pageNum, pageSize);
        IPage<CaseBrowseLog> logPage = caseBrowseLogMapper.selectByUserId(page, userId);

        List<CaseBrowseVO> voList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (CaseBrowseLog log : logPage.getRecords()) {
            FraudCase caseEntity = getById(log.getCaseId());
            if (caseEntity != null) {
                CaseBrowseVO vo = new CaseBrowseVO();
                vo.setCaseId(caseEntity.getId());
                vo.setCaseTitle(caseEntity.getTitle());
                vo.setCaseType(caseEntity.getCaseType());
                vo.setDifficultyLevel(caseEntity.getDifficultyLevel());
                vo.setBrowseTime(log.getBrowseTime().format(formatter));
                vo.setStayDuration(log.getStayDuration());
                vo.setStayDurationDesc(formatDuration(log.getStayDuration()));
                vo.setTagNames(getTagNamesByCaseId(caseEntity.getId()));
                voList.add(vo);
            }
        }

        Page<CaseBrowseVO> resultPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public List<CaseVO> getHotCases(int limit) {
        List<FraudCase> hotCases = baseMapper.selectHotCases(limit);
        return hotCases.stream().map(this::convertToCaseVO).collect(Collectors.toList());
    }

    /**
     * 威尔逊置信度得分计算公式
     * 用于根据点赞数和浏览量计算一个考虑样本量的置信度得分
     */
    @Override
    public double calculateWilsonScore(int positive, int total) {
        if (total == 0) return 0;
        double z = 1.645;
        double phat = (double) positive / total;
        double denominator = 1 + (z * z / total);
        double center = phat + (z * z / (2 * total));
        double spread = z * Math.sqrt((phat * (1 - phat) + (z * z / (4 * total))) / total);
        return (center - spread) / denominator;
    }

    /**
     * 目标年级匹配逻辑
     * 支持精确匹配和"all"(全部)通配符
     */
    @Override
    public boolean matchTargetGrade(String targetGrade, String userGrade) {
        if (targetGrade == null || targetGrade.isEmpty()) return true;
        if (targetGrade.contains("all")) return true;
        return targetGrade.equals(userGrade);
    }

    /**
     * 目标专业匹配逻辑
     * 支持列表匹配和"all"通配符
     */
    @Override
    public boolean matchTargetMajor(List<String> targetMajors, String userMajor) {
        if (targetMajors == null || targetMajors.isEmpty()) return true;
        if (targetMajors.contains("all")) return true;
        return targetMajors.contains(userMajor);
    }

    /**
     * 根据风险评分获取风险等级描述
     */
    @Override
    public String getRiskLevel(double riskScore) {
        if (riskScore >= 8) return "极高";
        if (riskScore >= 6) return "高";
        if (riskScore >= 4) return "中";
        if (riskScore >= 2) return "低";
        return "极低";
    }

    /**
     * 根据难度等级获取描述
     */
    @Override
    public String getDifficultyName(int level) {
        return switch (level) {
            case 1 -> "入门";
            case 2 -> "简单";
            case 3 -> "中等";
            case 4 -> "困难";
            case 5 -> "噩梦";
            default -> "未知";
        };
    }

    private void saveTagRelations(Long caseId, List<Long> tagIds) {
        for (Long tagId : tagIds) {
            CaseTagRelation relation = new CaseTagRelation();
            relation.setCaseId(caseId);
            relation.setTagId(tagId);
            caseTagRelationMapper.insert(relation);
        }
    }

    private void updateCaseLikeStats(Long caseId, Integer viewCount) {
        int likeCount = caseLikeMapper.countByCaseId(caseId);
        updateCaseLikeStats(caseId, viewCount, likeCount);
    }

    private void updateCaseLikeStats(Long caseId, Integer viewCount, Integer likeCountOverride) {
        int safeViewCount = viewCount == null ? 0 : viewCount;
        // 无 override 时以 case_like 表为准（全量同步）；有点赞/取消点赞传入的 override 时必须采用，
        // 否则历史种子数据仅写了 fraud_case.like_count、未同步 case_like 行时，会被 count=1 错误覆盖成 1。
        int likeCount = likeCountOverride != null
                ? likeCountOverride
                : caseLikeMapper.countByCaseId(caseId);
        BigDecimal likeRate = safeViewCount > 0 ? BigDecimal.valueOf((double) likeCount / safeViewCount) : BigDecimal.ZERO;
        double wilsonScore = calculateWilsonScore(likeCount, safeViewCount);
        baseMapper.updateLikeStats(caseId, likeCount, likeRate, BigDecimal.valueOf(wilsonScore));
    }

    private IPage<CaseVO> convertToCaseVOPage(IPage<FraudCase> casePage) {
        List<CaseVO> voList = casePage.getRecords().stream().map(this::convertToCaseVO).collect(Collectors.toList());
        Page<CaseVO> resultPage = new Page<>(casePage.getCurrent(), casePage.getSize(), casePage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    private CaseVO convertToCaseVO(FraudCase caseEntity) {
        CaseVO caseVO = new CaseVO();
        caseVO.setId(caseEntity.getId());
        caseVO.setTitle(caseEntity.getTitle());
        caseVO.setCaseType(caseEntity.getCaseType());
        caseVO.setContent(caseEntity.getContent());
        caseVO.setScripts(convertScriptsToJson(caseEntity.getScripts()));
        caseVO.setTargetGrades(caseEntity.getTargetGrades());
        caseVO.setTargetMajors(caseEntity.getTargetMajors());
        caseVO.setDifficultyLevel(caseEntity.getDifficultyLevel());
        caseVO.setDifficultyName(getDifficultyName(caseEntity.getDifficultyLevel()));
        caseVO.setRiskScore(caseEntity.getRiskScore());
        caseVO.setRiskLevel(getRiskLevel(caseEntity.getRiskScore().doubleValue()));
        caseVO.setViewCount(caseEntity.getViewCount());
        caseVO.setLikeCount(caseEntity.getLikeCount());
        caseVO.setLikeRate(caseEntity.getLikeRate());
        caseVO.setWilsonScore(caseEntity.getWilsonScore());
        caseVO.setIsFeatured(caseEntity.getIsFeatured());
        caseVO.setStatus(caseEntity.getStatus());
        caseVO.setPublishTime(caseEntity.getPublishTime());
        caseVO.setCreateTime(caseEntity.getCreateTime());
        caseVO.setTags(getTagsByCaseId(caseEntity.getId()));
        return caseVO;
    }

    private String convertScriptsToJson(Object scripts) {
        if (scripts == null) return null;
        if (scripts instanceof String s) return s;
        try {
            return OBJECT_MAPPER.writeValueAsString(scripts);
        } catch (JsonProcessingException e) {
            // 兜底：尽量返回可用字符串，避免接口直接 500
            log.warn("scripts JSON序列化失败，使用toString兜底: {}", e.getMessage());
            return String.valueOf(scripts);
        }
    }

    private List<TagVO> getTagsByCaseId(Long caseId) {
        List<Long> tagIds = caseTagRelationMapper.selectTagIdsByCaseId(caseId);
        if (tagIds.isEmpty()) return Collections.emptyList();
        List<CaseTag> tags = caseTagMapper.selectBatchIds(tagIds);
        return tags.stream().map(this::convertToTagVO).collect(Collectors.toList());
    }

    private List<String> getTagNamesByCaseId(Long caseId) {
        List<TagVO> tags = getTagsByCaseId(caseId);
        return tags.stream().map(TagVO::getName).collect(Collectors.toList());
    }

    private TagVO convertToTagVO(CaseTag tag) {
        TagVO tagVO = new TagVO();
        tagVO.setId(tag.getId());
        tagVO.setName(tag.getName());
        tagVO.setCategory(tag.getCategory());
        tagVO.setDescription(tag.getDescription());
        tagVO.setColor(tag.getColor());
        return tagVO;
    }

    private String formatDuration(int seconds) {
        if (seconds < 60) return seconds + "秒";
        if (seconds < 3600) {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return remainingSeconds > 0 ? minutes + "分" + remainingSeconds + "秒" : minutes + "分";
        }
        int hours = seconds / 3600;
        int remainingMinutes = (seconds % 3600) / 60;
        return remainingMinutes > 0 ? hours + "小时" + remainingMinutes + "分" : hours + "小时";
    }
}
