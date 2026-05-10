package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.dto.ScenarioDecisionRequest;
import com.anti.entity.vo.ScenarioProgressVO;
import com.anti.security.JwtUtils;
import com.anti.service.ScenarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 情景模拟控制器(FSM状态机)
 */
@RestController
@RequestMapping("/api/scenario")
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final JwtUtils jwtUtils;

    public ScenarioController(ScenarioService scenarioService, JwtUtils jwtUtils) {
        this.scenarioService = scenarioService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 开始情景模拟
     */
    @PostMapping("/start/{challengeId}")
    public Result<ScenarioProgressVO> startScenario(@PathVariable Long challengeId,
                                                     HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(scenarioService.startScenario(challengeId, userId));
    }

    /**
     * 获取当前进度
     */
    @GetMapping("/progress/{challengeId}")
    public Result<ScenarioProgressVO> getProgress(@PathVariable Long challengeId,
                                                  HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(scenarioService.getProgress(challengeId, userId));
    }

    /**
     * 做出决策
     */
    @PostMapping("/decision")
    public Result<ScenarioProgressVO> makeDecision(@RequestBody ScenarioDecisionRequest request,
                                                    HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        return Result.success(scenarioService.makeDecision(request, userId));
    }

    /**
     * 重置情景模拟
     */
    @PostMapping("/reset/{challengeId}")
    public Result<Void> resetScenario(@PathVariable Long challengeId,
                                       HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        scenarioService.resetScenario(challengeId, userId);
        return Result.success();
    }

    /**
     * 获取结局
     */
    @GetMapping("/ending/{challengeId}")
    public Result<ScenarioProgressVO> getEnding(@PathVariable Long challengeId,
                                                  HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(scenarioService.getEnding(challengeId, userId));
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
