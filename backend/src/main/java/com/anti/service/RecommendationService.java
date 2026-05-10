package com.anti.service;

import com.anti.entity.vo.RecommendationVO;
import com.anti.entity.vo.UserInterestVO;

import java.util.List;

/**
 * 推荐服务接口
 */
public interface RecommendationService {

    /**
     * 获取个性化推荐
     *
     * @param userId  用户ID
     * @param limit   推荐数量
     * @param itemType 推荐类型(可选: case/news/challenge)
     * @return 推荐结果列表
     */
    List<RecommendationVO> getRecommendations(Long userId, int limit, String itemType);

    /**
     * 获取新手期推荐(冷启动)
     *
     * @param userId 用户ID
     * @param limit  推荐数量
     * @return 推荐结果列表
     */
    List<RecommendationVO> getNewbieRecommendations(Long userId, int limit);

    /**
     * 获取成长期推荐(低活跃)
     *
     * @param userId 用户ID
     * @param limit  推荐数量
     * @return 推荐结果列表
     */
    List<RecommendationVO> getGrowingRecommendations(Long userId, int limit);

    /**
     * 获取成熟期推荐(高活跃)
     *
     * @param userId 用户ID
     * @param limit  推荐数量
     * @return 推荐结果列表
     */
    List<RecommendationVO> getMatureRecommendations(Long userId, int limit);

    /**
     * 获取用户兴趣分析
     *
     * @param userId 用户ID
     * @return 用户兴趣VO
     */
    UserInterestVO getUserInterestAnalysis(Long userId);

    /**
     * 记录推荐点击反馈
     *
     * @param userId  用户ID
     * @param itemId  推荐项ID
     * @param itemType 推荐项类型
     */
    void recordRecommendationClick(Long userId, Long itemId, String itemType);

    /**
     * 更新用户行为矩阵
     *
     * @param userId 用户ID
     * @param tagId  标签ID
     * @param score  行为得分增量
     */
    void updateUserBehaviorMatrix(Long userId, Long tagId, java.math.BigDecimal score);

    /**
     * 计算并更新用户相似度
     *
     * @param userId 用户ID
     */
    void calculateAndUpdateUserSimilarity(Long userId);

    /**
     * 批量计算所有用户的相似度(定时任务)
     */
    void batchCalculateUserSimilarities();
}
