package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.Challenge;
import com.anti.entity.FraudCase;
import com.anti.entity.News;
import com.anti.entity.RecommendationLog;
import com.anti.entity.UserBehaviorMatrix;
import com.anti.entity.UserProfile;
import com.anti.entity.vo.RecommendationVO;
import com.anti.entity.vo.UserInterestVO;
import com.anti.mapper.AssociationRuleMapper;
import com.anti.mapper.CaseBrowseLogMapper;
import com.anti.mapper.CaseTagMapper;
import com.anti.mapper.CaseTagRelationMapper;
import com.anti.mapper.ChallengeMapper;
import com.anti.mapper.FraudCaseMapper;
import com.anti.mapper.NewsMapper;
import com.anti.mapper.RecommendationLogMapper;
import com.anti.mapper.UserBehaviorMatrixMapper;
import com.anti.mapper.UserProfileMapper;
import com.anti.mapper.UserSimilarityMapper;
import com.anti.service.ProfileService;
import com.anti.util.RedisCacheUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private FraudCaseMapper fraudCaseMapper;
    @Mock
    private NewsMapper newsMapper;
    @Mock
    private ChallengeMapper challengeMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private UserBehaviorMatrixMapper behaviorMatrixMapper;
    @Mock
    private UserSimilarityMapper similarityMapper;
    @Mock
    private RecommendationLogMapper recommendationLogMapper;
    @Mock
    private AssociationRuleMapper associationRuleMapper;
    @Mock
    private CaseTagMapper caseTagMapper;
    @Mock
    private CaseTagRelationMapper caseTagRelationMapper;
    @Mock
    private CaseBrowseLogMapper caseBrowseLogMapper;
    @Mock
    private RedisCacheUtil redisCacheUtil;
    @Mock
    private ProfileService profileService;

    @InjectMocks
    private RecommendationServiceImpl service;

    @Test
    void getRecommendationsRejectsMissingUserBeforeProfileLookup() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getRecommendations(null, 10, null));

        assertThat(exception.getCode()).isEqualTo(401);
        assertThat(exception.getMessage()).contains("请先登录");
        verifyNoInteractions(userProfileMapper, newsMapper, fraudCaseMapper, challengeMapper);
    }

    @Test
    void recordRecommendationClickRejectsUnknownItemTypeBeforePersistence() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.recordRecommendationClick(1L, 10L, "video")
        );

        assertThat(exception.getCode()).isEqualTo(400);
        assertThat(exception.getMessage()).contains("推荐类型只能是case、news或challenge");
        verifyNoInteractions(recommendationLogMapper, fraudCaseMapper, behaviorMatrixMapper, redisCacheUtil);
    }

    @Test
    void getRecommendationsRejectsInvalidTypeBeforeProfileInitialization() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getRecommendations(1L, 10, "video")
        );

        assertThat(exception.getCode()).isEqualTo(400);
        assertThat(exception.getMessage()).contains("推荐类型只能是case、news或challenge");
        verifyNoInteractions(userProfileMapper, recommendationLogMapper);
    }

    @Test
    void newbieColdStartMixesMandatoryNewsHotCasesAndBasicChallenges() {
        when(userProfileMapper.selectByUserId(1L)).thenReturn(null);
        when(newsMapper.selectRequiredNews(1)).thenReturn(List.of(mandatoryNews()));
        when(fraudCaseMapper.selectList(any())).thenReturn(List.of(), List.of(hotCase()));
        when(challengeMapper.selectList(any())).thenReturn(List.of(basicChallenge()));
        when(recommendationLogMapper.findRecommendedCaseIds(1L)).thenReturn(List.of());
        when(recommendationLogMapper.findRecommendedNewsIds(1L)).thenReturn(List.of());
        when(recommendationLogMapper.findRecommendedChallengeIds(1L)).thenReturn(List.of());

        List<com.anti.entity.vo.RecommendationVO> result = service.getRecommendations(1L, 3, null);

        assertThat(result).extracting(com.anti.entity.vo.RecommendationVO::getItemType)
                .contains("news", "case", "challenge");
        verify(userProfileMapper).insert(any(UserProfile.class));
    }

    @Test
    void getRecommendationsReturnsCachedResultWithoutRecomputingOrLoggingExposure() {
        RecommendationVO cached = recommendation("case", 10L);
        when(redisCacheUtil.get("cache:recommend:1:type:case:limit:3")).thenReturn(List.of(cached));

        List<RecommendationVO> result = service.getRecommendations(1L, 3, "case");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemId()).isEqualTo(10L);
        verifyNoInteractions(userProfileMapper, newsMapper, fraudCaseMapper, challengeMapper, recommendationLogMapper);
    }

    @Test
    void recommendationExposureWriteFailureDoesNotBreakColdStartRecommendations() {
        when(userProfileMapper.selectByUserId(1L)).thenReturn(null);
        when(newsMapper.selectRequiredNews(1)).thenReturn(List.of(mandatoryNews()));
        when(fraudCaseMapper.selectList(any())).thenReturn(List.of(), List.of(hotCase()));
        when(challengeMapper.selectList(any())).thenReturn(List.of(basicChallenge()), List.of());
        when(recommendationLogMapper.insert(any(RecommendationLog.class))).thenThrow(new RuntimeException("log-secret"));

        List<com.anti.entity.vo.RecommendationVO> result = service.getRecommendations(1L, 3, null);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(com.anti.entity.vo.RecommendationVO::getItemType)
                .contains("news", "case", "challenge");
    }

    @Test
    void recordRecommendationClickCreatesFallbackLogAndUpdatesCaseTagBehavior() {
        when(fraudCaseMapper.selectById(10L)).thenReturn(hotCase());
        when(recommendationLogMapper.selectOne(any())).thenReturn(null);
        when(fraudCaseMapper.findTagIdsByCaseId(10L)).thenReturn(List.of(101L, 102L));
        when(behaviorMatrixMapper.findByUserAndTag(any(), any())).thenReturn(null);

        service.recordRecommendationClick(1L, 10L, "case");

        ArgumentCaptor<RecommendationLog> logCaptor = ArgumentCaptor.forClass(RecommendationLog.class);
        verify(recommendationLogMapper).insert(logCaptor.capture());
        RecommendationLog log = logCaptor.getValue();
        assertThat(log.getUserId()).isEqualTo(1L);
        assertThat(log.getItemId()).isEqualTo(10L);
        assertThat(log.getItemType()).isEqualTo("case");
        assertThat(log.getClicked()).isEqualTo(1);
        assertThat(log.getRecommendReason()).isEqualTo("[\"direct_click\"]");

        ArgumentCaptor<UserBehaviorMatrix> matrixCaptor = ArgumentCaptor.forClass(UserBehaviorMatrix.class);
        verify(behaviorMatrixMapper, never()).updateById(any());
        verify(behaviorMatrixMapper, org.mockito.Mockito.times(2)).insert(matrixCaptor.capture());
        assertThat(matrixCaptor.getAllValues())
                .extracting(UserBehaviorMatrix::getTagId)
                .containsExactly(101L, 102L);
        verify(redisCacheUtil, org.mockito.Mockito.times(2)).deleteUserInterest(1L);
        verify(redisCacheUtil).deleteUserRecommendations(1L);
    }

    @Test
    void recordRecommendationClickTreatsNullCaseTagsAsEmptyFeedback() {
        when(fraudCaseMapper.selectById(10L)).thenReturn(hotCase());
        when(recommendationLogMapper.selectOne(any())).thenReturn(null);
        when(fraudCaseMapper.findTagIdsByCaseId(10L)).thenReturn(null);

        service.recordRecommendationClick(1L, 10L, "case");

        verify(recommendationLogMapper).insert(any(RecommendationLog.class));
        verify(behaviorMatrixMapper, never()).insert(any());
        verify(behaviorMatrixMapper, never()).updateById(any());
        verify(redisCacheUtil).deleteUserRecommendations(1L);
    }

    @Test
    void getUserInterestAnalysisTreatsNullBehaviorMatrixAsEmptyTags() {
        UserProfile profile = new UserProfile();
        profile.setUserId(1L);
        profile.setLifecycleStage("newbie");
        profile.setKnowledgeLevel(20);

        when(userProfileMapper.selectByUserId(1L)).thenReturn(profile);
        when(profileService.determineLifecycleStage(1L)).thenReturn("newbie");
        when(behaviorMatrixMapper.findByUserId(1L)).thenReturn(null);

        UserInterestVO result = service.getUserInterestAnalysis(1L);

        assertThat(result.getLifecycleStage()).isEqualTo("newbie");
        assertThat(result.getInterestTags()).isEmpty();
        verify(userProfileMapper, never()).updateById(any());
        verify(redisCacheUtil).set(any(), any(UserInterestVO.class), anyLong(), any());
    }

    @Test
    void getUserInterestAnalysisReturnsCachedResultWithoutClearingCache() {
        UserInterestVO cached = new UserInterestVO();
        cached.setLifecycleStage("newbie");
        cached.setLifecycleStageName("新手期");
        cached.setKnowledgeLevel(30);
        cached.setWeakPoints(List.of());
        cached.setInterestTags(List.of());
        when(redisCacheUtil.get("cache:user_interest:1")).thenReturn(cached);

        UserInterestVO result = service.getUserInterestAnalysis(1L);

        assertThat(result.getLifecycleStage()).isEqualTo("newbie");
        assertThat(result.getKnowledgeLevel()).isEqualTo(30);
        verify(redisCacheUtil, never()).delete(anyString());
        verifyNoInteractions(userProfileMapper, profileService, behaviorMatrixMapper);
    }

    @Test
    void recordRecommendationClickRejectsDisabledNewsBeforeLogging() {
        News draft = mandatoryNews();
        draft.setStatus(0);
        when(newsMapper.selectById(20L)).thenReturn(draft);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.recordRecommendationClick(1L, 20L, "news"));

        assertThat(exception.getCode()).isEqualTo(404);
        verify(recommendationLogMapper, never()).insert(any());
        verify(recommendationLogMapper, never()).updateById(any());
    }

    private News mandatoryNews() {
        News news = new News();
        news.setId(20L);
        news.setTitle("必读预警");
        news.setSummary("全校必读");
        news.setNewsType("warning");
        news.setIsMandatory(1);
        news.setStatus(1);
        news.setViewCount(100);
        return news;
    }

    private FraudCase hotCase() {
        FraudCase fraudCase = new FraudCase();
        fraudCase.setId(10L);
        fraudCase.setTitle("热门案例");
        fraudCase.setCaseType("刷单返利");
        fraudCase.setContent("案例内容");
        fraudCase.setStatus(1);
        fraudCase.setViewCount(100);
        fraudCase.setLikeCount(10);
        return fraudCase;
    }

    private Challenge basicChallenge() {
        Challenge challenge = new Challenge();
        challenge.setId(30L);
        challenge.setTitle("基础关卡");
        challenge.setDescription("基础训练");
        challenge.setType("quiz");
        challenge.setStatus(1);
        challenge.setScoreReward(20);
        challenge.setLevelOrder(1);
        return challenge;
    }

    private RecommendationVO recommendation(String itemType, Long itemId) {
        RecommendationVO vo = new RecommendationVO();
        vo.setItemType(itemType);
        vo.setItemId(itemId);
        vo.setTitle("缓存推荐");
        return vo;
    }
}
