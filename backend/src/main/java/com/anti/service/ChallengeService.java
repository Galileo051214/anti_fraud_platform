package com.anti.service;

import com.anti.entity.dto.CreateChallengeRequest;
import com.anti.entity.dto.SubmitChallengeRequest;
import com.anti.entity.dto.UpdateChallengeRequest;
import com.anti.entity.vo.ChallengeOverviewVO;
import com.anti.entity.vo.ChallengeProgressVO;
import com.anti.entity.vo.ChallengeRecordVO;
import com.anti.entity.vo.ChallengeResultVO;
import com.anti.entity.vo.ChallengeStatsVO;
import com.anti.entity.vo.ChallengeVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 闯关服务接口
 */
public interface ChallengeService {

    /**
     * 获取闯关关卡列表(带用户状态)
     */
    List<ChallengeVO> getChallengeList(Long userId);

    /**
     * 获取关卡详情
     */
    ChallengeVO getChallengeDetail(Long challengeId, Long userId);

    /**
     * 获取闯关记录列表
     */
    IPage<ChallengeRecordVO> getChallengeRecords(Long userId, int pageNum, int pageSize);

    /**
     * 提交闯关答案
     */
    ChallengeResultVO submitChallenge(SubmitChallengeRequest request, Long userId);

    /**
     * 创建关卡(管理员)
     */
    ChallengeVO createChallenge(CreateChallengeRequest request);

    /**
     * 更新关卡(管理员)
     */
    ChallengeVO updateChallenge(Long challengeId, UpdateChallengeRequest request);

    /**
     * 删除关卡(管理员)
     */
    void deleteChallenge(Long challengeId);

    /**
     * 获取所有关卡(管理员)
     */
    IPage<ChallengeVO> getAdminChallengeList(int pageNum, int pageSize, String keyword, String type);

    /**
     * 获取闯关进度统计
     */
    ChallengeProgressVO getChallengeProgress(Long userId);

    /**
     * 获取关卡统计概览(管理员)
     */
    ChallengeOverviewVO getChallengeOverview();

    /**
     * 获取指定关卡的统计数据(管理员)
     */
    ChallengeStatsVO getChallengeStats(Long challengeId);

    /**
     * 批量启用/禁用关卡
     */
    void batchUpdateStatus(List<Long> challengeIds, Integer status);

    /**
     * 批量删除关卡
     */
    void batchDelete(List<Long> challengeIds);
}
