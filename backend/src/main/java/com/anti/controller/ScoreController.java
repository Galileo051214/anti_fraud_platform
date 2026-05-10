package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.dto.ScoreChangeRequest;
import com.anti.entity.vo.ScoreVO;
import com.anti.security.LoginUser;
import com.anti.service.AchievementService;
import com.anti.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/score")
@RequiredArgsConstructor
@Tag(name = "积分管理")
public class ScoreController {

    private final ScoreService scoreService;
    private final AchievementService achievementService;

    @GetMapping("/info")
    @Operation(summary = "获取积分信息")
    public Result<ScoreVO> getScoreInfo(@AuthenticationPrincipal LoginUser loginUser) {
        var score = scoreService.getScoreByUserId(loginUser.getUserId());
        var vo = new ScoreVO();
        vo.setId(score.getId());
        vo.setUserId(score.getUserId());
        vo.setTotalScore(score.getTotalScore());
        vo.setCurrentLevel(score.getCurrentLevel());
        vo.setWeeklyScore(score.getWeeklyScore());
        vo.setUpdateTime(score.getUpdateTime());
        vo.setUnlockedAchievements(achievementService.getUnlockedCount(loginUser.getUserId()));
        vo.setTotalAchievements(achievementService.getTotalAchievementCount());
        return Result.success(vo);
    }

    @PostMapping("/add")
    @Operation(summary = "增加积分(管理员)")
    public Result<Void> addScore(@RequestBody ScoreChangeRequest request, @AuthenticationPrincipal LoginUser loginUser) {
        if (!"admin".equals(loginUser.getRole())) {
            return Result.error(403, "权限不足");
        }
        scoreService.addScore(loginUser.getUserId(), request.getScore(), request.getReason());
        return Result.success();
    }

    @PostMapping("/deduct")
    @Operation(summary = "扣减积分(管理员)")
    public Result<Void> deductScore(@RequestBody ScoreChangeRequest request, @AuthenticationPrincipal LoginUser loginUser) {
        if (!"admin".equals(loginUser.getRole())) {
            return Result.error(403, "权限不足");
        }
        if (request.getScore() <= 0 || request.getScore() > 1000) {
            return Result.error(400, "扣减积分数量必须在1-1000之间");
        }
        scoreService.deductScore(loginUser.getUserId(), request.getScore(), request.getReason());
        return Result.success();
    }

    @GetMapping("/level/{level}")
    @Operation(summary = "获取指定等级需要的积分")
    public Result<Integer> getRequiredScore(@PathVariable Integer level) {
        if (level < 1) {
            return Result.error(400, "等级必须大于0");
        }
        int requiredScore = (level - 1) * scoreService.calculateLevel(level * 100);
        return Result.success(requiredScore);
    }
}
