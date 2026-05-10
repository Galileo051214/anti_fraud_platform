package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.Achievement;
import com.anti.entity.UserAchievement;
import com.anti.entity.vo.AchievementVO;
import com.anti.entity.vo.UserAchievementVO;
import com.anti.security.LoginUser;
import com.anti.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/achievement")
@RequiredArgsConstructor
@Tag(name = "成就管理")
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping("/list")
    @Operation(summary = "获取所有成就列表")
    public Result<List<AchievementVO>> getAllAchievements(@AuthenticationPrincipal LoginUser loginUser) {
        var achievements = achievementService.getAllAchievements();
        var userAchievements = achievementService.getUserAchievements(loginUser.getUserId());
        Set<Long> unlockedIds = userAchievements.stream()
                .map(UserAchievement::getAchievementId)
                .collect(Collectors.toSet());
        var voList = achievements.stream().map(a -> {
            var vo = new AchievementVO();
            vo.setId(a.getId());
            vo.setCode(a.getCode());
            vo.setName(a.getName());
            vo.setDescription(a.getDescription());
            vo.setIcon(a.getIcon());
            vo.setScoreReward(a.getScoreReward());
            vo.setConditionType(a.getConditionType());
            vo.setConditionValue(a.getConditionValue());
            vo.setCreateTime(a.getCreateTime());
            vo.setUnlocked(unlockedIds.contains(a.getId()));
            return vo;
        }).toList();
        return Result.success(voList);
    }

    @GetMapping("/user")
    @Operation(summary = "获取用户已解锁成就")
    public Result<List<UserAchievementVO>> getUserAchievements(@AuthenticationPrincipal LoginUser loginUser) {
        var userAchievements = achievementService.getUserAchievements(loginUser.getUserId());
        var voList = userAchievements.stream().map(ua -> {
            var vo = new UserAchievementVO();
            vo.setId(ua.getId());
            vo.setUserId(ua.getUserId());
            vo.setAchievementId(ua.getAchievementId());
            vo.setAchievedTime(ua.getAchievedTime());
            if (ua.getAchievement() != null) {
                vo.setAchievementCode(ua.getAchievement().getCode());
                vo.setAchievementName(ua.getAchievement().getName());
                vo.setDescription(ua.getAchievement().getDescription());
                vo.setIcon(ua.getAchievement().getIcon());
                vo.setScoreReward(ua.getAchievement().getScoreReward());
            }
            return vo;
        }).toList();
        return Result.success(voList);
    }

    @GetMapping("/user/count")
    @Operation(summary = "获取用户成就数量统计")
    public Result<Map<String, Object>> getUserAchievementCount(@AuthenticationPrincipal LoginUser loginUser) {
        int unlocked = achievementService.getUnlockedCount(loginUser.getUserId());
        int total = achievementService.getTotalAchievementCount();
        return Result.success(Map.of("unlocked", unlocked, "total", total));
    }

    @PostMapping("/unlock/{code}")
    @Operation(summary = "手动解锁成就(管理员)")
    public Result<Void> unlockAchievement(@PathVariable String code, @AuthenticationPrincipal LoginUser loginUser) {
        if (!"admin".equals(loginUser.getRole())) {
            return Result.error(403, "权限不足");
        }
        achievementService.unlockAchievement(loginUser.getUserId(), code);
        return Result.success();
    }
}
