package com.muying.ai.controller;

import com.alibaba.fastjson2.JSON;
import com.muying.ai.model.ChatRequest;
import com.muying.ai.service.ChatService;
import com.muying.ai.service.ModelRouterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 聊天控制器
 * 提供 SSE 流式和同步两种聊天接口
 */
@Slf4j
@Tag(name = "聊天接口", description = "流式 SSE 对话 / 同步对话 / 模型列表")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final String STREAM_ERROR_MESSAGE = "生成回答时出现异常，请稍后重试";

    private final ChatService chatService;
    private final ModelRouterService modelRouterService;

    public ChatController(ChatService chatService, ModelRouterService modelRouterService) {
        this.chatService = chatService;
        this.modelRouterService = modelRouterService;
    }

    /**
     * SSE 流式聊天接口
     * 前端通过 EventSource 接收逐 Token 响应
     */
    @Operation(summary = "SSE 流式聊天", description = "返回 text/event-stream，事件类型: start / message / error / done")
    @PostMapping(value = "/stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ChatRequest request) {
        ensureSessionId(request);

        return Flux.just(buildEvent("start", Map.of("sessionId", request.getSessionId())))
                .concatWith(chatService.streamChat(request)
                        .map(token -> ServerSentEvent.<String>builder()
                                .event("message")
                                .data(token)
                                .build())
                        .onErrorResume(ex -> {
                            log.error("SSE 聊天流异常, sessionId={}", request.getSessionId(), ex);
                            return Flux.just(buildEvent("error", Map.of(
                                    "code", "STREAM_ERROR",
                                    "message", STREAM_ERROR_MESSAGE,
                                    "sessionId", request.getSessionId())));
                        }))
                .concatWith(Flux.just(buildEvent("done", Map.of(
                        "code", "STREAM_COMPLETED",
                        "sessionId", request.getSessionId()))));
    }

    /**
     * 同步聊天接口（一次性返回完整响应）
     */
    @Operation(summary = "同步对话", description = "一次性返回完整回答，适用于非流式场景")
    @PostMapping
    public Map<String, String> chat(@Valid @RequestBody ChatRequest request) {
        ensureSessionId(request);
        String response = chatService.chat(request);
        return Map.of(
                "content", response,
                "sessionId", request.getSessionId());
    }

    /**
     * 获取可用模型列表
     */
    @Operation(summary = "获取可用模型列表")
    @GetMapping("/models")
    public Set<String> getAvailableModels() {
        return modelRouterService.getAvailableModels();
    }

    private void ensureSessionId(ChatRequest request) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            request.setSessionId(UUID.randomUUID().toString());
        }
    }

    private ServerSentEvent<String> buildEvent(String event, Map<String, Object> payload) {
        return ServerSentEvent.<String>builder()
                .event(event)
                .data(JSON.toJSONString(payload))
                .build();
    }
}
