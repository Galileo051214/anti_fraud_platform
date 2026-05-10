package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.dto.CreateCaseRequest;
import com.anti.entity.dto.UpdateCaseRequest;
import com.anti.entity.vo.CaseBrowseVO;
import com.anti.entity.vo.CaseVO;
import com.anti.security.JwtUtils;
import com.anti.service.FraudCaseService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 诈骗案例控制器
 */
@RestController
@RequestMapping("/api/case")
public class CaseController {

    private final FraudCaseService fraudCaseService;
    private final JwtUtils jwtUtils;

    public CaseController(FraudCaseService fraudCaseService, JwtUtils jwtUtils) {
        this.fraudCaseService = fraudCaseService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 分页查询案例列表
     */
    @GetMapping("/page")
    public Result<IPage<CaseVO>> getCasePage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword) {
        return Result.success(fraudCaseService.getCasePage(pageNum, pageSize, tagId, keyword));
    }

    /**
     * 获取案例详情
     */
    @GetMapping("/{id}")
    public Result<CaseVO> getCaseDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(fraudCaseService.getCaseDetail(id, userId));
    }

    /**
     * 创建案例
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CaseVO> createCase(@RequestBody CreateCaseRequest request, HttpServletRequest httpRequest) {
        Long authorId = getUserIdFromRequest(httpRequest);
        return Result.success(fraudCaseService.createCase(request, authorId));
    }

    /**
     * 更新案例
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CaseVO> updateCase(@PathVariable Long id, @RequestBody UpdateCaseRequest request) {
        return Result.success(fraudCaseService.updateCase(id, request));
    }

    /**
     * 删除案例
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCase(@PathVariable Long id) {
        fraudCaseService.deleteCase(id);
        return Result.success();
    }

    /**
     * 发布案例
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> publishCase(@PathVariable Long id) {
        fraudCaseService.publishCase(id);
        return Result.success();
    }

    /**
     * 设置精选
     */
    @PutMapping("/{id}/featured")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> setFeatured(@PathVariable Long id, @RequestParam int isFeatured) {
        fraudCaseService.setFeatured(id, isFeatured);
        return Result.success();
    }

    /**
     * 点赞案例
     */
    @PostMapping("/{id}/like")
    public Result<Void> likeCase(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        fraudCaseService.likeCase(id, userId);
        return Result.success();
    }

    /**
     * 取消点赞
     */
    @DeleteMapping("/{id}/like")
    public Result<Void> unlikeCase(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        fraudCaseService.unlikeCase(id, userId);
        return Result.success();
    }

    /**
     * 记录浏览
     */
    @PostMapping("/{id}/browse")
    public Result<Void> browseCase(@PathVariable Long id,
                                   @RequestParam(defaultValue = "0") int stayDuration,
                                   HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        fraudCaseService.browseCase(id, userId, stayDuration);
        return Result.success();
    }

    /**
     * 获取浏览记录
     */
    @GetMapping("/browse/history")
    public Result<IPage<CaseBrowseVO>> getBrowseHistory(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(fraudCaseService.getBrowseHistory(userId, pageNum, pageSize));
    }

    /**
     * 获取热度排行榜
     */
    @GetMapping("/hot")
    public Result<List<CaseVO>> getHotCases(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(fraudCaseService.getHotCases(limit));
    }

    /**
     * 获取威尔逊置信度得分
     */
    @GetMapping("/wilson")
    public Result<Double> getWilsonScore(@RequestParam int positive, @RequestParam int total) {
        return Result.success(fraudCaseService.calculateWilsonScore(positive, total));
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtils.getUserIdFromToken(token);
        }
        return null;
    }
}
