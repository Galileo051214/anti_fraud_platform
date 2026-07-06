package com.anti.controller;

import com.anti.common.BusinessException;
import com.anti.common.Result;
import com.anti.entity.dto.AgentChallengeReplyRequest;
import com.anti.entity.vo.AgentChallengeSessionVO;
import com.anti.security.LoginUser;
import com.anti.service.AgentChallengeService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Agent模拟挑战控制器
 */
@RestController
@RequestMapping("/api/agent-challenge")
public class AgentChallengeController {

    private final AgentChallengeService agentChallengeService;

    public AgentChallengeController(AgentChallengeService agentChallengeService) {
        this.agentChallengeService = agentChallengeService;
    }

    @PostMapping("/start/{challengeId}")
    public Result<AgentChallengeSessionVO> startChallenge(@PathVariable Long challengeId,
                                                          @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(agentChallengeService.startChallenge(challengeId, requireLogin(loginUser)));
    }

    @PostMapping(value = "/reply-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter replyStream(@Valid @RequestBody AgentChallengeReplyRequest request,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        SseEmitter emitter = new SseEmitter(120_000L);

        CompletableFuture.runAsync(() -> {
            try {
                agentChallengeService.replyStream(
                        request,
                        userId,
                        new AgentChallengeService.AgentChallengeStreamHandler() {
                            @Override
                            public void onMetadata(AgentChallengeSessionVO metadata) {
                                sendEventOrThrow(emitter, "meta", metadata);
                            }

                            @Override
                            public void onAgentDelta(String delta) {
                                sendEventOrThrow(emitter, "delta", Map.of("delta", delta));
                            }

                            @Override
                            public void onComplete(AgentChallengeSessionVO result) {
                                sendEventOrThrow(emitter, "done", result);
                            }
                        }
                );
                completeSilently(emitter);
            } catch (SseSendException e) {
                completeWithErrorSilently(emitter, e);
            } catch (BusinessException e) {
                sendEventSilently(emitter, "error", Map.of("message", e.getMessage()));
                completeSilently(emitter);
            } catch (Exception e) {
                sendEventSilently(emitter, "error", Map.of("message", "Agent模拟挑战暂时不可用，请稍后再试"));
                completeSilently(emitter);
            }
        });

        return emitter;
    }

    @GetMapping("/session/{sessionId}")
    public Result<AgentChallengeSessionVO> getSession(@PathVariable String sessionId,
                                                      @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(agentChallengeService.getSession(sessionId, requireLogin(loginUser)));
    }

    private Long requireLogin(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException(401, "请先登录");
        }
        return loginUser.getUserId();
    }

    private void sendEventOrThrow(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException e) {
            throw new SseSendException(e);
        }
    }

    private void sendEventSilently(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException ignored) {
            // 客户端断开后无法继续写入错误事件。
        }
    }

    private void completeSilently(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // 响应已结束。
        }
    }

    private void completeWithErrorSilently(SseEmitter emitter, Exception e) {
        try {
            emitter.completeWithError(e);
        } catch (IllegalStateException ignored) {
            // 响应已结束。
        }
    }

    private static class SseSendException extends RuntimeException {
        SseSendException(Throwable cause) {
            super(cause);
        }
    }
}
