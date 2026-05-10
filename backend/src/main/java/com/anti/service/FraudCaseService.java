package com.anti.service;

import com.anti.entity.dto.CreateCaseRequest;
import com.anti.entity.dto.UpdateCaseRequest;
import com.anti.entity.vo.CaseBrowseVO;
import com.anti.entity.vo.CaseVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 案例服务接口
 */
public interface FraudCaseService {

    /**
     * 分页查询案例列表
     */
    IPage<CaseVO> getCasePage(int pageNum, int pageSize, Long tagId, String keyword);

    /**
     * 获取案例详情
     */
    CaseVO getCaseDetail(Long caseId, Long userId);

    /**
     * 创建案例(管理员)
     */
    CaseVO createCase(CreateCaseRequest request, Long authorId);

    /**
     * 更新案例(管理员)
     */
    CaseVO updateCase(Long caseId, UpdateCaseRequest request);

    /**
     * 删除案例(管理员)
     */
    void deleteCase(Long caseId);

    /**
     * 发布案例(管理员)
     */
    void publishCase(Long caseId);

    /**
     * 设置精选(管理员)
     */
    void setFeatured(Long caseId, int isFeatured);

    /**
     * 点赞案例
     */
    void likeCase(Long caseId, Long userId);

    /**
     * 取消点赞
     */
    void unlikeCase(Long caseId, Long userId);

    /**
     * 记录浏览
     */
    void browseCase(Long caseId, Long userId, int stayDuration);

    /**
     * 获取浏览记录
     */
    IPage<CaseBrowseVO> getBrowseHistory(Long userId, int pageNum, int pageSize);

    /**
     * 获取热度排行榜
     */
    List<CaseVO> getHotCases(int limit);

    /**
     * 计算威尔逊置信度得分
     */
    double calculateWilsonScore(int positive, int total);

    /**
     * 目标年级匹配判断
     */
    boolean matchTargetGrade(String targetGrade, String userGrade);

    /**
     * 目标专业匹配判断
     */
    boolean matchTargetMajor(List<String> targetMajors, String userMajor);

    /**
     * 获取风险等级描述
     */
    String getRiskLevel(double riskScore);

    /**
     * 获取难度等级描述
     */
    String getDifficultyName(int level);
}
