package com.muying.ai.service;

import com.muying.ai.model.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 聊天核心服务
 * 组装 Spring AI Advisor 链（Memory）+ 手动 RAG 检索 + Function Calling
 */
@Slf4j
@Service
public class ChatService {

    private final ModelRouterService modelRouter;
    private final VectorStore vectorStore;
    private final ChatMemoryRepository chatMemoryRepository;
    private final RagAlertNotifier ragAlertNotifier;

    /** 按模型名缓存 ChatClient（ChatMemory 状态存 Redis，实例本身无状态可复用） */
    private final Map<String, ChatClient> chatClientCache = new ConcurrentHashMap<>();

    /** 系统提示词 — 母婴客服人设 */
    private static final String SYSTEM_PROMPT = """
            你是"萌宝助手"，母婴商城的智能客服。你的职责是：
            1. 回答用户关于母婴商品、育儿知识的问题
            2. 帮助用户查询订单状态、物流进度
            3. 介绍优惠活动和可用优惠券

            回答要求：
            - 语气温暖、专业、耐心
            - 涉及商品推荐时，优先考虑安全性和适用性
            - 如果不确定，请诚实告知用户，不要编造信息
            - 回答使用中文
            - 如果下方提供了"参考资料"，请优先基于参考资料回答
            """;

    public ChatService(ModelRouterService modelRouter,
                       VectorStore vectorStore,
                       ChatMemoryRepository chatMemoryRepository,
                       RagAlertNotifier ragAlertNotifier) {
        this.modelRouter = modelRouter;
        this.vectorStore = vectorStore;
        this.chatMemoryRepository = chatMemoryRepository;
        this.ragAlertNotifier = ragAlertNotifier;
    }

    /**
     * 流式聊天 — SSE 逐 Token 输出
     */
    public Flux<String> streamChat(ChatRequest request) {
        ChatClient chatClient = buildChatClient(request.getModel());
        String augmentedMessage = buildAugmentedMessage(request.getMessage());

        return chatClient.prompt()
                .user(augmentedMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.getSessionId()))
                .toolNames("orderQuery", "productQuery", "logisticsQuery", "couponQuery")
                .stream()
                .content();
    }

    /**
     * 同步聊天 — 一次性返回完整响应
     */
    public String chat(ChatRequest request) {
        ChatClient chatClient = buildChatClient(request.getModel());
        String augmentedMessage = buildAugmentedMessage(request.getMessage());

        return chatClient.prompt()
                .user(augmentedMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.getSessionId()))
                .toolNames("orderQuery", "productQuery", "logisticsQuery", "couponQuery")
                .call()
                .content();
    }

    /**
     * RAG 检索增强
     * 从 Milvus 向量库中检索相关文档，拼接到用户消息中
     */
    String buildAugmentedMessage(String userMessage) {
        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(userMessage)
                            .topK(3)
                            .similarityThreshold(0.6)
                            .build()
            );

            if (results == null || results.isEmpty()) {
                return userMessage;
            }

            String context = results.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n---\n\n"));
            String sources = results.stream()
                    .map(this::formatSource)
                    .distinct()
                    .collect(Collectors.joining("、"));

            return """
                    用户问题：%s

                    参考资料来源：%s

                    参考资料（来自知识库）：
                    %s
                    """.formatted(userMessage, sources, context);
        } catch (Exception e) {
            log.error("RAG 检索失败，已降级为纯对话模式。请检查向量库、Embedding 与知识库索引状态。", e);
            ragAlertNotifier.notifyRagDegraded(userMessage, e);
            return userMessage;
        }
    }

    String formatSource(Document document) {
        Object source = document.getMetadata().get("source");
        if (source == null) {
            return "未知来源";
        }
        String value = source.toString().trim();
        return value.isEmpty() ? "未知来源" : value;
    }

    /**
     * 获取（或创建并缓存）ChatClient
     */
    private ChatClient buildChatClient(String modelName) {
        return chatClientCache.computeIfAbsent(
                modelName == null || modelName.isBlank() ? "deepseek" : modelName,
                this::createChatClient);
    }

    private ChatClient createChatClient(String modelName) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();

        return ChatClient.builder(modelRouter.getModel(modelName))
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
