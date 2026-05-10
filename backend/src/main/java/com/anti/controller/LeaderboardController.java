package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.vo.LeaderboardVO;
import com.anti.security.JwtUtils;
import com.anti.service.LeaderboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 排行榜控制器
 */
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final JwtUtils jwtUtils;

    public LeaderboardController(LeaderboardService leaderboardService, JwtUtils jwtUtils) {
        this.leaderboardService = leaderboardService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 获取日排行榜
     */
    @GetMapping("/daily")
    public Result<List<LeaderboardVO>> getDailyLeaderboard(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(leaderboardService.getDailyLeaderboard(userId, limit));
    }

    /**
     * 获取周排行榜
     */
    @GetMapping("/weekly")
    public Result<List<LeaderboardVO>> getWeeklyLeaderboard(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(leaderboardService.getWeeklyLeaderboard(userId, limit));
    }

    /**
     * 获取总排行榜
     */
    @GetMapping("/all")
    public Result<List<LeaderboardVO>> getAllTimeLeaderboard(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(leaderboardService.getAllTimeLeaderboard(userId, limit));
    }

    /**
     * 获取用户排名
     */
    @GetMapping("/user-rank")
    public Result<LeaderboardVO> getUserRank(
            @RequestParam(defaultValue = "daily") String periodType,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(leaderboardService.getUserRank(userId, periodType));
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
