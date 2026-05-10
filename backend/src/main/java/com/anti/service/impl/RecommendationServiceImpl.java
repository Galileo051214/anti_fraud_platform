package com.anti.service.impl;

import com.anti.common.CacheConstants;
import com.anti.entity.*;
import com.anti.entity.vo.RecommendationVO;
import com.anti.entity.vo.UserInterestVO;
import com.anti.mapper.*;
import com.anti.service.ProfileService;
import com.anti.service.RecommendationService;
import com.anti.util.RedisCacheUtil;
import com.anti.util.RecommendationAlgorithmUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 推荐服务实现类
 * 实现混合推荐算法，根据用户生命周期阶段选择不同策略
 * 集成Redis缓存优化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final FraudCaseMapper fraudCaseMapper;
    private final NewsMapper newsMapper;
    private final ChallengeMapper challengeMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserBehaviorMatrixMapper behaviorMatrixMapper;
    private final UserSimilarityMapper similarityMapper;
    private final RecommendationLogMapper recommendationLogMapper;
    private final AssociationRuleMapper associationRuleMapper;
    private final CaseTagMapper caseTagMapper;
    private final CaseTagRelationMapper caseTagRelationMapper;
    private final CaseBrowseLogMapper caseBrowseLogMapper;
    private final RedisCacheUtil redisCacheUtil;
    private final ProfileService profileService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<RecommendationVO> getRecommendations(Long userId, int limit, String itemType) {
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null) {
            profile = initUserProfile(userId);
        }

        String lifecycleStage = profile.getLifecycleStage() != null ? profile.getLifecycleStage() : "newbie";
        log.info("用户{} 生命周期阶段: {}, itemType={}", userId, lifecycleStage, itemType);

        String type = itemType != null ? itemType.trim().toLowerCase() : "";
        if (!type.isEmpty()) {
            return getTypedRecommendations(userId, limit, type, lifecycleStage, profile);
        }

        return switch (lifecycleStage) {
            case "newbie" -> getNewbieRecommendations(userId, limit);
            case "growing" -> getGrowingRecommendations(userId, limit);
            case "mature" -> getMatureRecommendations(userId, limit);
            default -> getNewbieRecommendations(userId, limit);
        };
    }

    /**
     * 按内容类型返回推荐：案例 / 资讯 / 闯关，策略仍随生命周期变化（需求文档：新手期规则+热度、成长期内容+SPM、成熟期协同过滤）
     */
    private List<RecommendationVO> getTypedRecommendations(Long userId, int limit, String itemType,
                                                         String lifecycleStage, UserProfile profile) {
        return switch (itemType) {
            case "news" -> getNewsRecommendationsTyped(userId, limit, lifecycleStage, profile);
            case "challenge" -> getChallengeRecommendationsTyped(userId, limit, lifecycleStage, profile);
            case "case" -> getCaseRecommendationsTyped(userId, limit, lifecycleStage, profile);
            default -> getCaseRecommendationsTyped(userId, limit, lifecycleStage, profile);
        };
    }

    private List<RecommendationVO> getCaseRecommendationsTyped(Long userId, int limit, String lifecycle, UserProfile profile) {
        int fetchLimit = Math.max(limit * 3, 18);
        List<RecommendationVO> base = switch (lifecycle) {
            case "growing" -> getGrowingRecommendations(userId, fetchLimit);
            case "mature" -> getMatureRecommendations(userId, fetchLimit);
            default -> getNewbieRecommendations(userId, fetchLimit);
        };
        List<RecommendationVO> cases = base.stream()
                .filter(v -> "case".equals(v.getItemType()))
                .collect(Collectors.toList());
        List<RecommendationVO> sorted = deduplicateAndSort(cases, userId, limit);
        if (sorted.size() < limit) {
            Set<Long> have = sorted.stream().map(RecommendationVO::getItemId).filter(Objects::nonNull).collect(Collectors.toSet());
            for (FraudCase c : getHotCases(limit * 2)) {
                if (sorted.size() >= limit) {
                    break;
                }
                if (have.contains(c.getId())) {
                    continue;
                }
                RecommendationVO vo = convertCaseToVO(c, "hot_supplement");
                vo.setScore(calculateNewbieScore(c, profile, false));
                sorted.add(vo);
                have.add(c.getId());
            }
            sorted = deduplicateAndSort(sorted, userId, limit);
        }
        return sorted;
    }

    private List<RecommendationVO> getNewsRecommendationsTyped(Long userId, int limit, String lifecycle, UserProfile profile) {
        List<RecommendationVO> out = new ArrayList<>();
        out.addAll(getMandatoryNews(Math.min(limit, 5)));

        if ("newbie".equals(lifecycle) || "growing".equals(lifecycle)) {
            List<News> topNews = newsMapper.selectList(
                    new QueryWrapper<News>().eq("status", 1).eq("is_top", 1)
                            .orderByDesc("publish_time").last("LIMIT " + limit));
            for (News n : topNews) {
                out.add(convertNewsToVO(n, List.of("置顶策略")));
            }
            List<News> warnings = newsMapper.selectList(
                    new QueryWrapper<News>().eq("status", 1).eq("news_type", "warning")
                            .orderByDesc("publish_time").last("LIMIT " + limit));
            for (News n : warnings) {
                out.add(convertNewsToVO(n, List.of("紧急预警")));
            }
        }

        if ("growing".equals(lifecycle) || "mature".equals(lifecycle)) {
            List<UserBehaviorMatrix> behaviors = new ArrayList<>(behaviorMatrixMapper.findByUserId(userId));
            behaviors.sort((a, b) -> b.getBehaviorScore().compareTo(a.getBehaviorScore()));
            for (UserBehaviorMatrix b : behaviors.stream().limit(3).collect(Collectors.toList())) {
                String tagName = getTagName(b.getTagId());
                if (tagName == null || tagName.isEmpty()) {
                    continue;
                }
                List<News> matched = newsMapper.selectList(new LambdaQueryWrapper<News>()
                        .eq(News::getStatus, 1)
                        .and(q -> q.like(News::getTitle, tagName).or().like(News::getSummary, tagName))
                        .orderByDesc(News::getViewCount)
                        .last("LIMIT 6"));
                for (News n : matched) {
                    out.add(convertNewsToVO(n, List.of("基于兴趣标签（" + tagName + "）")));
                }
            }
        }

        List<News> hot = newsMapper.selectList(
                new QueryWrapper<News>().eq("status", 1).orderByDesc("view_count").last("LIMIT " + (limit * 2)));
        for (News n : hot) {
            out.add(convertNewsToVO(n, List.of("浏览热度")));
        }

        Map<Long, RecommendationVO> uniq = new LinkedHashMap<>();
        for (RecommendationVO vo : out) {
            if (vo.getItemId() != null) {
                uniq.putIfAbsent(vo.getItemId(), vo);
            }
        }
        List<RecommendationVO> merged = new ArrayList<>(uniq.values());
        merged.sort((a, b) -> {
            BigDecimal sa = a.getScore() != null ? a.getScore() : BigDecimal.ZERO;
            BigDecimal sb = b.getScore() != null ? b.getScore() : BigDecimal.ZERO;
            return sb.compareTo(sa);
        });
        return deduplicateAndSort(merged, userId, limit);
    }

    private List<RecommendationVO> getChallengeRecommendationsTyped(Long userId, int limit, String lifecycle, UserProfile profile) {
        List<RecommendationVO> out = new ArrayList<>();
        if ("newbie".equals(lifecycle)) {
            for (Challenge c : getScenarioChallenges(limit)) {
                out.add(convertChallengeToVO(c, "新手期·情景模拟优先（动态上下文）"));
            }
            List<Challenge> quiz = challengeMapper.selectList(
                    new QueryWrapper<Challenge>().eq("status", 1).ne("type", "scenario")
                            .orderByAsc("level_order").last("LIMIT " + limit));
            for (Challenge c : quiz) {
                out.add(convertChallengeToVO(c, "新手期·知识闯关"));
            }
        } else if ("growing".equals(lifecycle)) {
            List<Challenge> all = challengeMapper.selectList(
                    new QueryWrapper<Challenge>().eq("status", 1).orderByAsc("level_order"));
            int kl = profile.getKnowledgeLevel() != null ? profile.getKnowledgeLevel() : 40;
            int targetDiff = Math.min(5, Math.max(1, kl / 25 + 1));
            for (Challenge c : all) {
                if (out.size() >= limit) {
                    break;
                }
                int d = c.getDifficulty() != null ? c.getDifficulty() : 1;
                if (Math.abs(d - targetDiff) <= 2) {
                    out.add(convertChallengeToVO(c, "成长期·难度与知识水平匹配"));
                }
            }
            for (Challenge c : all) {
                if (out.size() >= limit) {
                    break;
                }
                if (out.stream().noneMatch(v -> c.getId().equals(v.getItemId()))) {
                    out.add(convertChallengeToVO(c, "成长期·闯关进阶"));
                }
            }
        } else {
            List<Challenge> all = challengeMapper.selectList(
                    new QueryWrapper<Challenge>().eq("status", 1).orderByDesc("score_reward"));
            for (Challenge c : all.stream().limit(limit).collect(Collectors.toList())) {
                out.add(convertChallengeToVO(c, "成熟期·高价值关卡"));
            }
        }
        return deduplicateAndSort(out, userId, limit);
    }

    private RecommendationVO convertNewsToVO(News n, List<String> reasons) {
        RecommendationVO vo = new RecommendationVO();
        vo.setItemId(n.getId());
        vo.setItemType("news");
        vo.setTitle(n.getTitle());
        vo.setCoverImage(n.getCoverImage());
        vo.setSummary(n.getSummary());
        vo.setReasons(reasons);
        int vc = n.getViewCount() != null ? n.getViewCount() : 0;
        vo.setScore(BigDecimal.valueOf(vc));
        List<String> tags = new ArrayList<>();
        if (n.getNewsType() != null) {
            tags.add(switch (n.getNewsType()) {
                case "warning" -> "预警";
                case "policy" -> "政策";
                default -> "资讯";
            });
        }
        if (n.getIsTop() != null && n.getIsTop() == 1) {
            tags.add("置顶");
        }
        if (n.getIsMandatory() != null && n.getIsMandatory() == 1) {
            tags.add("必读");
        }
        vo.setTags(tags);
        return vo;
    }

    /**
     * 新手期推荐(冷启动策略)
     * - 静态属性匹配(年级+专业)
     * - 上下文感知规则过滤
     * - 动态属性优先级(情景模拟>知识测试)
     * - 强相关+通用型内容筛选
     * - 威尔逊得分区间算法
     * - 热度排行
     * - 必读文件置顶
     */
    @Override
    public List<RecommendationVO> getNewbieRecommendations(Long userId, int limit) {
        log.info("执行新手期推荐策略, userId={}", userId);

        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null) {
            profile = initUserProfile(userId);
        }

        String userGrade = profile.getGrade();
        String userMajor = profile.getMajor();
        List<RecommendationVO> results = new ArrayList<>();

        // 1. 优先推荐必读资讯(全校必修内容)
        results.addAll(getMandatoryNews(limit / 3));

        // 2. 筛选适合用户年级和专业的内容(静态属性匹配)
        List<FraudCase> matchingCases = filterCasesByTarget(profile, limit);
        for (FraudCase c : matchingCases) {
            RecommendationVO vo = convertCaseToVO(c, "grade_major_match");
            vo.setScore(calculateNewbieScore(c, profile, true));
            results.add(vo);
        }

        // 3. 如果匹配内容不足，补充通用型热门内容
        if (results.size() < limit) {
            List<FraudCase> hotCases = getHotCases(limit - results.size());
            for (FraudCase c : hotCases) {
                RecommendationVO vo = convertCaseToVO(c, "hot_recommend");
                vo.setScore(calculateNewbieScore(c, profile, false));
                results.add(vo);
            }
        }

        // 4. 情景模拟优先(动态属性优先级)
        List<Challenge> scenarios = getScenarioChallenges(limit / 2);
        for (Challenge ch : scenarios) {
            RecommendationVO vo = convertChallengeToVO(ch, "scenario_priority");
            results.add(vo);
        }

        // 5. 排序并去重
        return deduplicateAndSort(results, userId, limit);
    }

    /**
     * 成长期推荐(基于内容+协同过滤混合策略)
     * - TF-IDF关键词提取
     * - 用户兴趣向量构建
     * - 余弦相似度匹配
     * - 候选池生成(TOP N)
     * - SPM算法
     * - 关联规则库查询
     * - 诈骗连环套路预测
     * - 静态/动态上下文修正
     * - 综合得分计算
     */
    @Override
    public List<RecommendationVO> getGrowingRecommendations(Long userId, int limit) {
        log.info("执行成长期推荐策略, userId={}", userId);

        UserProfile profile = userProfileMapper.selectByUserId(userId);
        List<UserBehaviorMatrix> userTags = behaviorMatrixMapper.findByUserId(userId);

        Map<Long, BigDecimal> userInterestVector = buildInterestVector(userTags);
        List<String> userTagNames = extractTagNames(userTags);
        List<String> tagHistory = getTagHistory(userId);

        List<RecommendationVO> results = new ArrayList<>();

        // 1. 基于内容推荐 - TF-IDF + 余弦相似度
        List<FraudCase> contentBased = getContentBasedRecommendations(userInterestVector, limit);
        for (FraudCase c : contentBased) {
            BigDecimal similarity = calculateCaseSimilarity(c, userInterestVector);
            RecommendationVO vo = convertCaseToVO(c, "content_based");
            vo.setScore(similarity);
            results.add(vo);
        }

        // 2. SPM序列模式挖掘 - 关联规则预测
        List<String> predictedTags = spmPredict(userTagNames, tagHistory);
        if (!predictedTags.isEmpty()) {
            List<FraudCase> spmCases = getCasesByTags(predictedTags, limit / 2);
            for (FraudCase c : spmCases) {
                RecommendationVO vo = convertCaseToVO(c, "spm_predict");
                vo.setScore(vo.getScore().multiply(BigDecimal.valueOf(0.9)));
                results.add(vo);
            }
        }

        // 3. 上下文修正
        results = applyContextCorrection(results, profile);

        // 4. 综合得分计算
        results = calculateCompositeScore(results, profile);

        // 5. 排序并去重
        return deduplicateAndSort(results, userId, limit);
    }

    /**
     * 成熟期推荐(协同过滤策略)
     * - 上下文预过滤(同圈层用户)
     * - 皮尔逊相关系数
     * - K近邻查找
     * - 基于偏差的加权平均预测
     * - 候选池生成
     * - 序列重排
     */
    @Override
    public List<RecommendationVO> getMatureRecommendations(Long userId, int limit) {
        log.info("执行成熟期推荐策略, userId={}", userId);

        UserProfile profile = userProfileMapper.selectByUserId(userId);
        Map<Long, BigDecimal> userInterestVector = buildInterestVector(behaviorMatrixMapper.findByUserId(userId));

        List<RecommendationVO> results = new ArrayList<>();

        // 1. 上下文预过滤 - 找同圈层用户(相同年级、专业)
        List<Long> similarUsers = findSimilarCircleUsers(userId, profile);

        // 2. K近邻查找
        List<UserSimilarity> neighbors = findKNearestNeighbors(userId, similarUsers, 20);

        // 3. 基于偏差的加权平均预测
        Map<Long, BigDecimal> neighborPredictions = predictWithBiasWeightedAverage(
                userId, userInterestVector, neighbors, limit * 2);

        // 4. 转换为推荐结果
        for (Map.Entry<Long, BigDecimal> entry : neighborPredictions.entrySet()) {
            FraudCase c = fraudCaseMapper.selectById(entry.getKey());
            if (c != null) {
                RecommendationVO vo = convertCaseToVO(c, "collaborative_filtering");
                vo.setScore(entry.getValue());
                results.add(vo);
            }
        }

        // 5. 序列重排
        results = reorderBySequence(results, userId);

        // 6. 去重
        return deduplicateAndSort(results, userId, limit);
    }

    @Override
    public UserInterestVO getUserInterestAnalysis(Long userId) {
        log.info("=== getUserInterestAnalysis 开始: userId={} ===", userId);
        String cacheKey = CacheConstants.getUserInterestKey(userId);
        
        redisCacheUtil.delete(cacheKey);
        log.info("=== 已清除缓存，强制重新计算 ===");
        
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null) {
            profile = initUserProfile(userId);
        }
        
        log.info("=== 调用 determineLifecycleStage ===");
        String lifecycleStage = profileService.determineLifecycleStage(userId);
        log.info("=== determineLifecycleStage 返回: {} ===", lifecycleStage);
        if (!lifecycleStage.equals(profile.getLifecycleStage())) {
            profile.setLifecycleStage(lifecycleStage);
            userProfileMapper.updateById(profile);
        }
        
        List<UserBehaviorMatrix> behaviors = behaviorMatrixMapper.findByUserId(userId);

        UserInterestVO vo = new UserInterestVO();
        vo.setLifecycleStage(lifecycleStage);
        vo.setLifecycleStageName(getLifecycleStageName(lifecycleStage));
        vo.setKnowledgeLevel(profile.getKnowledgeLevel() != null ? profile.getKnowledgeLevel() : 0);
        vo.setWeakPoints(parseWeakPoints(profile.getWeakPoints()));

        List<UserInterestVO.TagScore> interestTags = new ArrayList<>();
        Map<Long, BigDecimal> userVector = new HashMap<>();
        for (UserBehaviorMatrix b : behaviors) {
            UserInterestVO.TagScore ts = new UserInterestVO.TagScore();
            ts.setTagId(b.getTagId());
            ts.setTagName(getTagName(b.getTagId()));
            ts.setScore(b.getBehaviorScore());
            interestTags.add(ts);
            userVector.put(b.getTagId(), b.getBehaviorScore());
        }
        interestTags.sort((a, b) -> b.getScore().compareTo(a.getScore()));
        vo.setInterestTags(interestTags);

        BigDecimal maxScore = userVector.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        if (maxScore.compareTo(BigDecimal.ZERO) > 0) {
            List<UserInterestVO.TagScore> normalized = new ArrayList<>();
            for (UserInterestVO.TagScore ts : interestTags) {
                UserInterestVO.TagScore nts = new UserInterestVO.TagScore();
                nts.setTagId(ts.getTagId());
                nts.setTagName(ts.getTagName());
                nts.setScore(ts.getScore().divide(maxScore, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
                normalized.add(nts);
            }
            vo.setInterestTags(normalized);
        }

        redisCacheUtil.set(cacheKey, vo, CacheConstants.USER_INTEREST_CACHE_TTL, TimeUnit.SECONDS);
        return vo;
    }

    @Override
    public void recordRecommendationClick(Long userId, Long itemId, String itemType) {
        LambdaQueryWrapper<RecommendationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecommendationLog::getUserId, userId)
                .eq(RecommendationLog::getItemId, itemId)
                .eq(RecommendationLog::getItemType, itemType)
                .orderByDesc(RecommendationLog::getCreateTime)
                .last("LIMIT 1");
        RecommendationLog log = recommendationLogMapper.selectOne(wrapper);
        if (log != null) {
            log.setClicked(1);
            recommendationLogMapper.updateById(log);
        }
    }

    @Override
    public void updateUserBehaviorMatrix(Long userId, Long tagId, BigDecimal score) {
        UserBehaviorMatrix existing = behaviorMatrixMapper.findByUserAndTag(userId, tagId);
        if (existing != null) {
            existing.setBehaviorScore(existing.getBehaviorScore().add(score));
            behaviorMatrixMapper.updateById(existing);
        } else {
            UserBehaviorMatrix matrix = new UserBehaviorMatrix();
            matrix.setUserId(userId);
            matrix.setTagId(tagId);
            matrix.setBehaviorScore(score);
            behaviorMatrixMapper.insert(matrix);
        }

        // 清除用户兴趣缓存
        redisCacheUtil.deleteUserInterest(userId);
    }

    @Override
    public void calculateAndUpdateUserSimilarity(Long userId) {
        UserBehaviorMatrix target = behaviorMatrixMapper.findByUserAndTag(userId, 0L);
        if (target == null) {
            return;
        }

        Map<Long, BigDecimal> targetVector = new HashMap<>();
        List<UserBehaviorMatrix> targetBehaviors = behaviorMatrixMapper.findByUserId(userId);
        for (UserBehaviorMatrix b : targetBehaviors) {
            targetVector.put(b.getTagId(), b.getBehaviorScore());
        }

        QueryWrapper<UserBehaviorMatrix> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT user_id");
        List<Object> userIds = behaviorMatrixMapper.selectObjs(wrapper);

        for (Object obj : userIds) {
            Long otherId = ((Number) obj).longValue();
            if (otherId.equals(userId)) continue;

            Map<Long, BigDecimal> otherVector = new HashMap<>();
            List<UserBehaviorMatrix> otherBehaviors = behaviorMatrixMapper.findByUserId(otherId);
            for (UserBehaviorMatrix b : otherBehaviors) {
                otherVector.put(b.getTagId(), b.getBehaviorScore());
            }

            BigDecimal similarity = RecommendationAlgorithmUtil.cosineSimilarity(targetVector, otherVector);

            UserSimilarity existing = similarityMapper.findByUserPair(userId, otherId);
            if (existing == null) {
                existing = similarityMapper.findByUserPair(otherId, userId);
            }

            if (existing != null) {
                existing.setSimilarityScore(similarity);
                existing.setCommonTags(buildCommonTagsJson(targetVector, otherVector));
                similarityMapper.updateById(existing);
            } else {
                UserSimilarity sim = new UserSimilarity();
                sim.setUserIdA(userId);
                sim.setUserIdB(otherId);
                sim.setSimilarityScore(similarity);
                sim.setCommonTags(buildCommonTagsJson(targetVector, otherVector));
                similarityMapper.insert(sim);
            }
        }
    }

    @Override
    public void batchCalculateUserSimilarities() {
        log.info("开始批量计算用户相似度");
        QueryWrapper<UserBehaviorMatrix> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT user_id");
        List<Object> userIds = behaviorMatrixMapper.selectObjs(wrapper);

        for (Object obj : userIds) {
            Long userId = ((Number) obj).longValue();
            try {
                calculateAndUpdateUserSimilarity(userId);
            } catch (Exception e) {
                log.error("计算用户{}相似度失败", userId, e);
            }
        }
        log.info("用户相似度批量计算完成");
    }

    // ==================== 私有辅助方法 ====================

    private UserProfile initUserProfile(Long userId) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setLifecycleStage("newbie");
        profile.setKnowledgeLevel(0);
        profile.setBrowseCount(0);
        profile.setRegisterDays(0);
        userProfileMapper.insert(profile);
        return profile;
    }

    private List<FraudCase> filterCasesByTarget(UserProfile profile, int limit) {
        QueryWrapper<FraudCase> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .orderByDesc("wilson_score")
                .last("LIMIT " + (limit * 2));

        List<FraudCase> cases = fraudCaseMapper.selectList(wrapper);
        return cases.stream()
                .filter(c -> matchTargetAudience(c, profile))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private boolean matchTargetAudience(FraudCase c, UserProfile profile) {
        List<String> targetGrades = c.getTargetGrades();
        List<String> targetMajors = c.getTargetMajors();

        if ((targetGrades == null || targetGrades.isEmpty()) && (targetMajors == null || targetMajors.isEmpty())) {
            return true;
        }

        boolean gradeMatch = matchGrades(targetGrades, profile.getGrade());
        boolean majorMatch = matchMajors(targetMajors, profile.getMajor());

        return gradeMatch && majorMatch;
    }

    private boolean matchGrades(List<String> targetGrades, String userGrade) {
        if (targetGrades == null || targetGrades.isEmpty() || targetGrades.contains("all")) {
            return true;
        }
        return targetGrades.contains(userGrade);
    }

    private boolean matchMajors(List<String> targetMajors, String userMajor) {
        if (targetMajors == null || targetMajors.isEmpty() || targetMajors.contains("all")) {
            return true;
        }
        return targetMajors.contains(userMajor);
    }

    private List<String> parseStringArray(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析JSON失败: {}", json);
            return Collections.emptyList();
        }
    }

    private List<FraudCase> getHotCases(int limit) {
        return fraudCaseMapper.selectList(
                new QueryWrapper<FraudCase>()
                        .eq("status", 1)
                        .orderByDesc("view_count")
                        .last("LIMIT " + limit)
        );
    }

    private List<Challenge> getScenarioChallenges(int limit) {
        return challengeMapper.selectList(
                new QueryWrapper<Challenge>()
                        .eq("status", 1)
                        .eq("type", "scenario")
                        .orderByAsc("level_order")
                        .last("LIMIT " + limit)
        );
    }

    private List<RecommendationVO> getMandatoryNews(int limit) {
        List<News> news = newsMapper.selectList(
                new QueryWrapper<News>()
                        .eq("status", 1)
                        .eq("is_mandatory", 1)
                        .orderByDesc("publish_time")
                        .last("LIMIT " + limit)
        );
        return news.stream()
                .map(n -> {
                    RecommendationVO vo = convertNewsToVO(n, List.of("全校必读（置顶策略）"));
                    vo.setScore(BigDecimal.valueOf(1_000_000));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calculateNewbieScore(FraudCase c, UserProfile profile, boolean isStrongMatch) {
        BigDecimal wilsonScore = c.getWilsonScore() != null ? c.getWilsonScore() : BigDecimal.ZERO;
        BigDecimal hotScore = RecommendationAlgorithmUtil.calculateHotScore(
                c.getViewCount(), c.getLikeCount(), 0);

        double weight = isStrongMatch ? 0.6 : 0.3;
        return wilsonScore.multiply(BigDecimal.valueOf(weight))
                .add(hotScore.multiply(BigDecimal.valueOf(1 - weight)));
    }

    private Map<Long, BigDecimal> buildInterestVector(List<UserBehaviorMatrix> behaviors) {
        Map<Long, BigDecimal> vector = new HashMap<>();
        for (UserBehaviorMatrix b : behaviors) {
            vector.put(b.getTagId(), b.getBehaviorScore());
        }
        return vector;
    }

    private List<String> extractTagNames(List<UserBehaviorMatrix> behaviors) {
        List<String> names = new ArrayList<>();
        for (UserBehaviorMatrix b : behaviors) {
            CaseTag tag = caseTagMapper.selectById(b.getTagId());
            if (tag != null) {
                names.add(tag.getName());
            }
        }
        return names;
    }

    private List<String> getTagHistory(Long userId) {
        List<CaseBrowseLog> logs = caseBrowseLogMapper.selectList(
                new LambdaQueryWrapper<CaseBrowseLog>()
                        .eq(CaseBrowseLog::getUserId, userId)
                        .orderByDesc(CaseBrowseLog::getBrowseTime)
                        .last("LIMIT 50")
        );

        Set<Long> caseIds = logs.stream()
                .map(CaseBrowseLog::getCaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (caseIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: get tag IDs via case_tag_relation
        List<CaseTagRelation> relations = caseTagRelationMapper.selectList(
                new LambdaQueryWrapper<CaseTagRelation>()
                        .in(CaseTagRelation::getCaseId, caseIds)
        );

        Set<Long> tagIds = relations.stream()
                .map(CaseTagRelation::getTagId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 2: get tag names from case_tag
        List<CaseTag> tags = caseTagMapper.selectList(
                new LambdaQueryWrapper<CaseTag>()
                        .in(CaseTag::getId, tagIds)
        );

        return tags.stream()
                .map(CaseTag::getName)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<FraudCase> getContentBasedRecommendations(Map<Long, BigDecimal> userVector, int limit) {
        List<FraudCase> allCases = fraudCaseMapper.selectList(
                new QueryWrapper<FraudCase>().eq("status", 1)
        );

        return allCases.stream()
                .sorted((c1, c2) -> {
                    BigDecimal s1 = calculateCaseSimilarity(c1, userVector);
                    BigDecimal s2 = calculateCaseSimilarity(c2, userVector);
                    return s2.compareTo(s1);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    private BigDecimal calculateCaseSimilarity(FraudCase c, Map<Long, BigDecimal> userVector) {
        if (c.getId() == null) {
            return BigDecimal.ZERO;
        }
        List<Long> caseTags = fraudCaseMapper.findTagIdsByCaseId(c.getId());
        Map<Long, BigDecimal> caseVector = new HashMap<>();
        for (Long tagId : caseTags) {
            caseVector.put(tagId, BigDecimal.ONE);
        }
        return RecommendationAlgorithmUtil.cosineSimilarity(userVector, caseVector);
    }

    private List<String> spmPredict(List<String> userTags, List<String> tagHistory) {
        Map<String, List<String>> rules = associationRuleMapper.findAllActiveRules().stream()
                .collect(Collectors.toMap(
                        AssociationRule::getTriggerTag,
                        r -> parseStringArray(r.getPredictedTags()),
                        (a, b) -> a
                ));

        List<String> predictions = new ArrayList<>();
        for (String tag : userTags) {
            List<String> predicted = RecommendationAlgorithmUtil.spmPredict(tag, tagHistory, rules);
            predictions.addAll(predicted);
        }
        return predictions.stream().distinct().collect(Collectors.toList());
    }

    private List<FraudCase> getCasesByTags(List<String> tags, int limit) {
        List<FraudCase> results = new ArrayList<>();
        for (String tagName : tags) {
            CaseTag tag = caseTagMapper.selectOne(
                    new LambdaQueryWrapper<CaseTag>().eq(CaseTag::getName, tagName)
            );
            if (tag != null) {
                List<Long> caseIds = fraudCaseMapper.findCaseIdsByTagId(tag.getId());
                for (Long caseId : caseIds) {
                    FraudCase c = fraudCaseMapper.selectById(caseId);
                    if (c != null && c.getStatus() == 1) {
                        results.add(c);
                        if (results.size() >= limit) {
                            return results;
                        }
                    }
                }
            }
        }
        return results;
    }

    private List<RecommendationVO> applyContextCorrection(List<RecommendationVO> results, UserProfile profile) {
        List<String> weak = parseWeakPoints(profile.getWeakPoints());
        for (RecommendationVO vo : results) {
            BigDecimal contextScore = BigDecimal.valueOf(1.0);

            if (!weak.isEmpty() && vo.getTags() != null) {
                for (String tag : vo.getTags()) {
                    if (weak.contains(tag)) {
                        contextScore = contextScore.multiply(BigDecimal.valueOf(1.2));
                    }
                }
            }

            if (profile.getKnowledgeLevel() < 30) {
                if (vo.getReasons() != null && vo.getReasons().contains("simple")) {
                    contextScore = contextScore.multiply(BigDecimal.valueOf(1.1));
                }
            }

            vo.setScore(vo.getScore().multiply(contextScore));
        }
        return results;
    }

    private List<RecommendationVO> calculateCompositeScore(List<RecommendationVO> results, UserProfile profile) {
        for (RecommendationVO vo : results) {
            BigDecimal contentScore = vo.getScore();
            BigDecimal contextScore = calculateContextScore(vo, profile);
            BigDecimal hotScore = vo.getScore().multiply(BigDecimal.valueOf(0.5));

            BigDecimal composite = RecommendationAlgorithmUtil.calculateCompositeScore(
                    contentScore, contextScore, hotScore, 0.5, 0.3, 0.2
            );
            vo.setScore(composite);
        }
        return results;
    }

    private BigDecimal calculateContextScore(RecommendationVO vo, UserProfile profile) {
        BigDecimal score = BigDecimal.ONE;
        List<String> weak = parseWeakPoints(profile.getWeakPoints());
        if (!weak.isEmpty() && vo.getTags() != null) {
            for (String tag : vo.getTags()) {
                if (weak.contains(tag)) {
                    score = score.add(BigDecimal.valueOf(0.1));
                }
            }
        }

        return score;
    }

    private List<Long> findSimilarCircleUsers(Long userId, UserProfile profile) {
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getGrade, profile.getGrade())
                .ne(UserProfile::getUserId, userId);
        List<UserProfile> circle = userProfileMapper.selectList(wrapper);
        return circle.stream().map(UserProfile::getUserId).collect(Collectors.toList());
    }

    private List<UserSimilarity> findKNearestNeighbors(Long userId, List<Long> candidateUsers, int k) {
        List<UserSimilarity> all = new ArrayList<>();
        for (Long candidateId : candidateUsers) {
            UserSimilarity sim = similarityMapper.findByUserPair(userId, candidateId);
            if (sim == null) {
                sim = similarityMapper.findByUserPair(candidateId, userId);
            }
            if (sim != null && sim.getSimilarityScore() != null) {
                all.add(sim);
            }
        }
        all.sort((a, b) -> b.getSimilarityScore().compareTo(a.getSimilarityScore()));
        return all.stream().limit(k).collect(Collectors.toList());
    }

    private Map<Long, BigDecimal> predictWithBiasWeightedAverage(
            Long userId, Map<Long, BigDecimal> userVector,
            List<UserSimilarity> neighbors, int limit) {

        Map<Long, BigDecimal> predictions = new HashMap<>();
        Map<Long, List<BigDecimal>> neighborRatings = new HashMap<>();

        for (UserSimilarity neighbor : neighbors) {
            Long neighborId = neighbor.getUserIdA().equals(userId) ?
                    neighbor.getUserIdB() : neighbor.getUserIdA();
            BigDecimal similarity = neighbor.getSimilarityScore();

            List<UserBehaviorMatrix> neighborBehaviors = behaviorMatrixMapper.findByUserId(neighborId);
            for (UserBehaviorMatrix b : neighborBehaviors) {
                if (!userVector.containsKey(b.getTagId())) {
                    neighborRatings.computeIfAbsent(b.getTagId(), k -> new ArrayList<>());
                    neighborRatings.get(b.getTagId()).add(b.getBehaviorScore().multiply(similarity));
                }
            }
        }

        BigDecimal avgUserScore = userVector.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(userVector.size()), 4, RoundingMode.HALF_UP);

        for (Map.Entry<Long, List<BigDecimal>> entry : neighborRatings.entrySet()) {
            BigDecimal sumWeighted = entry.getValue().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sumWeight = neighbors.stream()
                    .map(UserSimilarity::getSimilarityScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (sumWeight.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal predicted = sumWeighted.divide(sumWeight, 4, RoundingMode.HALF_UP)
                        .add(avgUserScore);
                predictions.put(entry.getKey(), predicted);
            }
        }

        return predictions.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

    private List<RecommendationVO> reorderBySequence(List<RecommendationVO> results, Long userId) {
        return results.stream()
                .sorted((a, b) -> {
                    BigDecimal sa = a.getScore() != null ? a.getScore() : BigDecimal.ZERO;
                    BigDecimal sb = b.getScore() != null ? b.getScore() : BigDecimal.ZERO;
                    return sb.compareTo(sa);
                })
                .collect(Collectors.toList());
    }

    private List<RecommendationVO> deduplicateAndSort(List<RecommendationVO> results, Long userId, int limit) {
        Set<Long> excludedCases = new HashSet<>(recommendationLogMapper.findRecommendedCaseIds(userId));
        Set<Long> excludedNews = new HashSet<>(recommendationLogMapper.findRecommendedNewsIds(userId));
        Set<Long> excludedCh = new HashSet<>(recommendationLogMapper.findRecommendedChallengeIds(userId));

        return results.stream()
                .filter(vo -> {
                    if (vo.getItemId() == null) {
                        return false;
                    }
                    String t = vo.getItemType();
                    if ("news".equals(t)) {
                        return !excludedNews.contains(vo.getItemId());
                    }
                    if ("challenge".equals(t)) {
                        return !excludedCh.contains(vo.getItemId());
                    }
                    return !excludedCases.contains(vo.getItemId());
                })
                .sorted((a, b) -> {
                    BigDecimal sa = a.getScore() != null ? a.getScore() : BigDecimal.ZERO;
                    BigDecimal sb = b.getScore() != null ? b.getScore() : BigDecimal.ZERO;
                    return sb.compareTo(sa);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    private RecommendationVO convertCaseToVO(FraudCase c, String reason) {
        RecommendationVO vo = new RecommendationVO();
        vo.setItemId(c.getId());
        vo.setItemType("case");
        vo.setTitle(c.getTitle());
        String raw = c.getContent() != null ? c.getContent().replaceAll("<[^>]+>", "") : "";
        vo.setSummary(raw.length() > 120 ? raw.substring(0, 120) + "…" : raw);
        vo.setScore(c.getWilsonScore() != null ? c.getWilsonScore() : BigDecimal.ZERO);
        vo.setReasons(List.of(reason));
        vo.setTags(buildCaseDisplayTags(c));
        return vo;
    }

    private List<String> buildCaseDisplayTags(FraudCase c) {
        List<String> tags = new ArrayList<>(getCaseTagNames(c.getId()));
        if (tags.isEmpty() && c.getCaseType() != null && !c.getCaseType().isBlank()) {
            tags.add(c.getCaseType());
        }
        if (c.getRiskScore() != null) {
            double r = c.getRiskScore().doubleValue();
            if (r >= 7) {
                tags.add("高风险");
            } else if (r >= 4) {
                tags.add("中风险");
            } else {
                tags.add("低风险");
            }
        }
        return tags;
    }

    private List<String> getCaseTagNames(Long caseId) {
        if (caseId == null) {
            return new ArrayList<>();
        }
        List<Long> tagIds = fraudCaseMapper.findTagIdsByCaseId(caseId);
        List<String> names = new ArrayList<>();
        for (Long tid : tagIds) {
            CaseTag t = caseTagMapper.selectById(tid);
            if (t != null && t.getName() != null) {
                names.add(t.getName());
            }
        }
        return names;
    }

    private RecommendationVO convertChallengeToVO(Challenge c, String reason) {
        RecommendationVO vo = new RecommendationVO();
        vo.setItemId(c.getId());
        vo.setItemType("challenge");
        vo.setTitle(c.getTitle());
        vo.setSummary(c.getDescription());
        vo.setScore(BigDecimal.valueOf(c.getScoreReward() != null ? c.getScoreReward() : 0));
        vo.setReasons(List.of(reason));
        List<String> tags = new ArrayList<>();
        if (c.getType() != null) {
            tags.add("scenario".equalsIgnoreCase(c.getType()) ? "情景模拟" : "知识闯关");
        }
        if (c.getDifficulty() != null) {
            tags.add("难度" + c.getDifficulty());
        }
        vo.setTags(tags);
        return vo;
    }

    private String getTagName(Long tagId) {
        CaseTag tag = caseTagMapper.selectById(tagId);
        return tag != null ? tag.getName() : "";
    }

    private List<String> parseWeakPoints(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析弱点标签失败: {}", json);
            return Collections.emptyList();
        }
    }

    private String getLifecycleStageName(String stage) {
        return switch (stage) {
            case "newbie" -> "新手期";
            case "growing" -> "成长期";
            case "mature" -> "成熟期";
            default -> "未知";
        };
    }

    private String buildCommonTagsJson(Map<Long, BigDecimal> v1, Map<Long, BigDecimal> v2) {
        Set<Long> common = v1.keySet();
        common.retainAll(v2.keySet());
        try {
            return objectMapper.writeValueAsString(new ArrayList<>(common));
        } catch (Exception e) {
            return "[]";
        }
    }
}
