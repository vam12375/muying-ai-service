package com.muying.ai.controller;

import com.muying.ai.model.ChatRequest;
import com.muying.ai.service.ChatService;
import com.muying.ai.service.ModelRouterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "聊天接口", description = "流式 SSE 对话 / 同步对话 / 模型列表")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

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
    @Operation(summary = "SSE 流式聊天", description = "返回 text/event-stream，事件类型: message(内容块) / done(结束标志)")
    @PostMapping(value = "/stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ChatRequest request) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            request.setSessionId(UUID.randomUUID().toString());
        }

        return chatService.streamChat(request)
                .map(token -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(token)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("[DONE]")
                                .build()
                ));
    }

    /**
     * 同步聊天接口（一次性返回完整响应）
     */
    @Operation(summary = "同步对话", description = "一次性返回完整回答，适用于非流式场景")
    @PostMapping
    public Map<String, String> chat(@Valid @RequestBody ChatRequest request) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            request.setSessionId(UUID.randomUUID().toString());
        }
        String response = chatService.chat(request);
        return Map.of(
                "content", response,
                "sessionId", request.getSessionId()
        );
    }

    /**
     * 获取可用模型列表
     */
    @Operation(summary = "获取可用模型列表")
    @GetMapping("/models")
    public Set<String> getAvailableModels() {
        return modelRouterService.getAvailableModels();
    }
}
