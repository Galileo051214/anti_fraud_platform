package com.anti.controller;

import com.anti.common.Result;
import com.anti.common.BusinessException;
import com.anti.entity.dto.BatchDeleteRequest;
import com.anti.entity.dto.BatchStatusRequest;
import com.anti.entity.dto.CreateChallengeRequest;
import com.anti.entity.dto.SubmitChallengeRequest;
import com.anti.entity.dto.UpdateChallengeRequest;
import com.anti.entity.vo.ChallengeOverviewVO;
import com.anti.entity.vo.ChallengeProgressVO;
import com.anti.entity.vo.ChallengeRecordVO;
import com.anti.entity.vo.ChallengeResultVO;
import com.anti.entity.vo.ChallengeStatsVO;
import com.anti.entity.vo.ChallengeVO;
import com.anti.security.JwtUtils;
import com.anti.service.ChallengeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 闯关控制器
 */
@RestController
@RequestMapping("/api/challenge")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final JwtUtils jwtUtils;

    public ChallengeController(ChallengeService challengeService, JwtUtils jwtUtils) {
        this.challengeService = challengeService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 获取闯关关卡列表
     */
    @GetMapping("/list")
    public Result<List<ChallengeVO>> getChallengeList(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(challengeService.getChallengeList(userId));
    }

    /**
     * 获取关卡详情
     */
    @GetMapping("/{id}")
    public Result<ChallengeVO> getChallengeDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(challengeService.getChallengeDetail(id, userId));
    }

    /**
     * 获取闯关记录
     */
    @GetMapping("/records")
    public Result<IPage<ChallengeRecordVO>> getChallengeRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(challengeService.getChallengeRecords(userId, pageNum, pageSize));
    }

    /**
     * 提交闯关答案
     */
    @PostMapping("/submit")
    public Result<ChallengeResultVO> submitChallenge(@RequestBody SubmitChallengeRequest request,
                                                     HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        return Result.success(challengeService.submitChallenge(request, userId));
    }

    /**
     * 获取闯关进度统计
     */
    @GetMapping("/progress")
    public Result<ChallengeProgressVO> getChallengeProgress(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(challengeService.getChallengeProgress(userId));
    }

    /**
     * 创建关卡(管理员)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ChallengeVO> createChallenge(@RequestBody CreateChallengeRequest request) {
        return Result.success(challengeService.createChallenge(request));
    }

    /**
     * 更新关卡(管理员)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ChallengeVO> updateChallenge(@PathVariable Long id,
                                               @RequestBody UpdateChallengeRequest request) {
        return Result.success(challengeService.updateChallenge(id, request));
    }

    /**
     * 删除关卡(管理员)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteChallenge(@PathVariable Long id) {
        challengeService.deleteChallenge(id);
        return Result.success();
    }

    /**
     * 获取所有关卡(管理员)
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<ChallengeVO>> getAdminChallengeList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {
        return Result.success(challengeService.getAdminChallengeList(pageNum, pageSize, keyword, type));
    }

    /**
     * 获取关卡统计概览(管理员)
     */
    @GetMapping("/admin/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ChallengeOverviewVO> getChallengeOverview() {
        return Result.success(challengeService.getChallengeOverview());
    }

    /**
     * 获取指定关卡的统计数据(管理员)
     */
    @GetMapping("/admin/stats/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ChallengeStatsVO> getChallengeStats(@PathVariable Long id) {
        return Result.success(challengeService.getChallengeStats(id));
    }

    /**
     * 批量启用/禁用关卡
     */
    @PutMapping("/admin/batch/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchUpdateStatus(@RequestBody BatchStatusRequest request) {
        challengeService.batchUpdateStatus(request.getChallengeIds(), request.getStatus());
        return Result.success();
    }

    /**
     * 批量删除关卡
     */
    @DeleteMapping("/admin/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchDelete(@RequestBody BatchDeleteRequest request) {
        challengeService.batchDelete(request.getChallengeIds());
        return Result.success();
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                return jwtUtils.getUserIdFromToken(token);
            } catch (Exception e) {
                // Token 无效/解析失败时不要抛 500，直接返回可读的业务错误
                throw new BusinessException("Token解析失败，请重新登录");
            }
        }
        throw new BusinessException("请先登录");
    }
}
