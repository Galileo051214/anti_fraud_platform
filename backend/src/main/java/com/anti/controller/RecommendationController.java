package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.dto.CreateAssociationRuleRequest;
import com.anti.entity.vo.AssociationRuleVO;
import com.anti.entity.vo.RecommendationVO;
import com.anti.entity.vo.UserInterestVO;
import com.anti.security.JwtUtils;
import com.anti.service.AssociationRuleService;
import com.anti.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐控制器
 * 提供个性化推荐、用户兴趣分析、关联规则管理接口
 */
@Tag(name = "推荐管理", description = "个性化推荐相关接口")
@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final AssociationRuleService associationRuleService;
    private final JwtUtils jwtUtils;

    @Operation(summary = "获取个性化推荐", description = "根据用户生命周期阶段自动选择推荐策略")
    @GetMapping("/list")
    public Result<List<RecommendationVO>> getRecommendations(
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "推荐类型: case/news/challenge") @RequestParam(required = false) String itemType,
            HttpServletRequest request) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        List<RecommendationVO> recommendations = recommendationService.getRecommendations(userId, limit, itemType);
        return Result.success(recommendations);
    }

    @Operation(summary = "获取新手期推荐", description = "冷启动策略，适合新用户")
    @GetMapping("/newbie")
    public Result<List<RecommendationVO>> getNewbieRecommendations(
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        List<RecommendationVO> recommendations = recommendationService.getNewbieRecommendations(userId, limit);
        return Result.success(recommendations);
    }

    @Operation(summary = "获取成长期推荐", description = "基于内容+协同过滤混合策略")
    @GetMapping("/growing")
    public Result<List<RecommendationVO>> getGrowingRecommendations(
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        List<RecommendationVO> recommendations = recommendationService.getGrowingRecommendations(userId, limit);
        return Result.success(recommendations);
    }

    @Operation(summary = "获取成熟期推荐", description = "协同过滤策略，适合活跃用户")
    @GetMapping("/mature")
    public Result<List<RecommendationVO>> getMatureRecommendations(
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        List<RecommendationVO> recommendations = recommendationService.getMatureRecommendations(userId, limit);
        return Result.success(recommendations);
    }

    @Operation(summary = "获取用户兴趣分析", description = "获取用户当前画像和兴趣标签")
    @GetMapping("/interest")
    public Result<UserInterestVO> getUserInterest(HttpServletRequest request) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        UserInterestVO interest = recommendationService.getUserInterestAnalysis(userId);
        return Result.success(interest);
    }

    @Operation(summary = "记录推荐点击", description = "用户点击推荐内容后调用，用于反馈优化")
    @PostMapping("/click")
    public Result<Void> recordClick(
            @Parameter(description = "推荐项ID") @RequestParam Long itemId,
            @Parameter(description = "推荐项类型") @RequestParam String itemType,
            HttpServletRequest request) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }

        recommendationService.recordRecommendationClick(userId, itemId, itemType);
        return Result.success(null);
    }

    @Operation(summary = "获取所有关联规则", description = "管理员查看所有关联规则")
    @GetMapping("/rules")
    public Result<List<AssociationRuleVO>> getAllRules() {
        List<AssociationRuleVO> rules = associationRuleService.list().stream()
                .map(rule -> {
                    AssociationRuleVO vo = new AssociationRuleVO();
                    vo.setId(rule.getId());
                    vo.setTriggerTag(rule.getTriggerTag());
                    vo.setConfidence(rule.getConfidence());
                    vo.setDescription(rule.getDescription());
                    vo.setStatus(rule.getStatus());
                    return vo;
                })
                .toList();
        return Result.success(rules);
    }

    @Operation(summary = "根据触发标签获取规则", description = "获取指定标签的关联预测规则")
    @GetMapping("/rules/{triggerTag}")
    public Result<List<AssociationRuleVO>> getRulesByTriggerTag(
            @PathVariable String triggerTag) {
        List<AssociationRuleVO> rules = associationRuleService.getRulesByTriggerTag(triggerTag);
        return Result.success(rules);
    }

    @Operation(summary = "创建关联规则", description = "管理员创建新的关联规则")
    @PostMapping("/rule")
    public Result<AssociationRuleVO> createRule(@RequestBody CreateAssociationRuleRequest request) {
        AssociationRuleVO rule = associationRuleService.createRule(request);
        return Result.success(rule);
    }

    @Operation(summary = "更新关联规则", description = "管理员更新关联规则")
    @PutMapping("/rule/{id}")
    public Result<AssociationRuleVO> updateRule(
            @PathVariable Long id,
            @RequestBody CreateAssociationRuleRequest request) {
        AssociationRuleVO rule = associationRuleService.updateRule(id, request);
        return Result.success(rule);
    }

    @Operation(summary = "删除关联规则", description = "管理员删除关联规则")
    @DeleteMapping("/rule/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        associationRuleService.removeById(id);
        return Result.success(null);
    }

    @Operation(summary = "批量计算用户相似度", description = "管理员触发批量计算所有用户的相似度(定时任务)")
    @PostMapping("/calculate-similarity")
    public Result<Void> calculateSimilarities() {
        recommendationService.batchCalculateUserSimilarities();
        return Result.success(null);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                return jwtUtils.getUserIdFromToken(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
