package com.anti.controller;

import com.anti.common.BusinessException;
import com.anti.common.Result;
import com.anti.entity.dto.ChatRequest;
import com.anti.entity.dto.FeedbackRequest;
import com.anti.entity.vo.ChatVO;
import com.anti.entity.vo.SessionVO;
import com.anti.entity.vo.TokenStatsVO;
import com.anti.security.LoginUser;
import com.anti.service.QAConversationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 智能客服控制器
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final QAConversationService qaConversationService;

    public ChatController(QAConversationService qaConversationService) {
        this.qaConversationService = qaConversationService;
    }

    /**
     * 发送问题并获取AI回答
     */
    @PostMapping("/ask")
    public Result<ChatVO> ask(@Valid @RequestBody ChatRequest request,
                              @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        ChatVO result = qaConversationService.askQuestion(
                request.getQuestion(),
                request.getSessionId(),
                userId,
                request.resolveAnswerType()
        );
        return Result.success(result);
    }

    /**
     * 流式发送问题并获取AI回答。
     */
    @PostMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@Valid @RequestBody ChatRequest request,
                                @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        SseEmitter emitter = new SseEmitter(120_000L);

        CompletableFuture.runAsync(() -> {
            try {
                qaConversationService.askQuestionStream(
                        request.getQuestion(),
                        request.getSessionId(),
                        userId,
                        request.resolveAnswerType(),
                        new QAConversationService.ChatStreamHandler() {
                            @Override
                            public void onMetadata(ChatVO metadata) {
                                sendEventOrThrow(emitter, "meta", metadata);
                            }

                            @Override
                            public void onReasoningDelta(String delta) {
                                sendEventOrThrow(emitter, "reasoning", Map.of("delta", delta));
                            }

                            @Override
                            public void onContentDelta(String delta) {
                                sendEventOrThrow(emitter, "delta", Map.of("delta", delta));
                            }

                            @Override
                            public void onComplete(ChatVO result) {
                                sendEventOrThrow(emitter, "done", result);
                            }
                        }
                );
                completeSilently(emitter);
            } catch (SseSendException e) {
                completeWithErrorSilently(emitter, e);
            } catch (Exception e) {
                sendEventSilently(emitter, "error", Map.of("message", "发送失败，请稍后再试"));
                completeSilently(emitter);
            }
        });

        return emitter;
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/history/{sessionId}")
    public Result<List<ChatVO>> getHistory(@PathVariable String sessionId,
                                           @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        List<ChatVO> history = qaConversationService.getConversationHistory(sessionId, userId);
        return Result.success(history);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public Result<List<SessionVO>> getSessions(@AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        List<SessionVO> sessions = qaConversationService.getSessionList(userId);
        return Result.success(sessions);
    }

    /**
     * 提交反馈
     */
    @PostMapping("/feedback")
    public Result<Void> submitFeedback(@Valid @RequestBody FeedbackRequest request,
                                       @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        qaConversationService.submitFeedback(request.getSessionId(), request.getFeedback(), userId);
        return Result.success();
    }

    /**
     * 获取Token统计
     */
    @GetMapping("/stats")
    public Result<TokenStatsVO> getStats(@AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        TokenStatsVO stats = qaConversationService.getTokenStats(userId);
        return Result.success(stats);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId,
                                       @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        qaConversationService.deleteSession(sessionId, userId);
        return Result.success();
    }

    /**
     * 创建新会话
     */
    @PostMapping("/new-session")
    public Result<String> createSession(@AuthenticationPrincipal LoginUser loginUser) {
        Long userId = requireLogin(loginUser);
        String sessionId = qaConversationService.createSession(userId);
        return Result.success(sessionId);
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
            // 客户端断开或响应已提交时无法再写入错误事件。
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
