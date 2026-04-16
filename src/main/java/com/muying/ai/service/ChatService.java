package com.muying.ai.service;

import com.muying.ai.model.ChatRequest;
import com.muying.ai.tool.ToolRequestContextHolder;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    private static final String TOOL_ORDER_QUERY = "orderQuery";
    private static final String TOOL_PRODUCT_QUERY = "productQuery";
    private static final String TOOL_LOGISTICS_QUERY = "logisticsQuery";
    private static final String TOOL_COUPON_QUERY = "couponQuery";
    private static final int RAG_TOP_K = 3;
    private static final double RAG_SIMILARITY_THRESHOLD = 0.6d;
    private static final Set<String> ORDER_TOOL_SCENARIOS = Set.of(
            "GENERAL_ASSISTANT", "ORDER_SERVICE", "LOGISTICS_SERVICE", "AFTER_SALES");
    private static final Set<String> COUPON_TOOL_SCENARIOS = Set.of(
            "GENERAL_ASSISTANT", "PROMOTION_SERVICE", "MEMBER_SERVICE");

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
        List<String> toolNames = resolveToolNames(request);
        String modelName = normalizeModelName(request.getModel());
        String maskedUserId = maskUserId(request.getUserId());
        String loginStatus = normalizeLoginStatus(request.getLoginStatus());
        String scenario = normalizeScenario(request.getScenario());

        return Flux.defer(() -> {
            long startedAt = System.currentTimeMillis();
            log.info(
                    "开始流式聊天, sessionId={}, model={}, userId={}, loginStatus={}, scenario={}, tools={}, messageLength={}",
                    request.getSessionId(), modelName, maskedUserId, loginStatus, scenario, toolNames,
                    request.getMessage() == null ? 0 : request.getMessage().length());

            ToolRequestContextHolder.set(request);
            var prompt = chatClient.prompt()
                    .user(augmentedMessage)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.getSessionId()));

            Flux<String> responseFlux = toolNames.isEmpty()
                    ? prompt.stream().content()
                    : prompt.toolNames(toolNames.toArray(String[]::new)).stream().content();

            return responseFlux
                    .doOnComplete(() -> log.info(
                            "流式聊天完成, sessionId={}, model={}, userId={}, loginStatus={}, scenario={}, tools={}, durationMs={}",
                            request.getSessionId(), modelName, maskedUserId, loginStatus, scenario, toolNames,
                            System.currentTimeMillis() - startedAt))
                    .doOnError(ex -> log.error(
                            "流式聊天失败, sessionId={}, model={}, userId={}, loginStatus={}, scenario={}, tools={}, durationMs={}",
                            request.getSessionId(), modelName, maskedUserId, loginStatus, scenario, toolNames,
                            System.currentTimeMillis() - startedAt, ex))
                    .doFinally(signalType -> ToolRequestContextHolder.clear());
        });
    }

    /**
     * 同步聊天 — 一次性返回完整响应
     */
    public String chat(ChatRequest request) {
        ChatClient chatClient = buildChatClient(request.getModel());
        String augmentedMessage = buildAugmentedMessage(request.getMessage());
        List<String> toolNames = resolveToolNames(request);
        String modelName = normalizeModelName(request.getModel());
        String maskedUserId = maskUserId(request.getUserId());
        String loginStatus = normalizeLoginStatus(request.getLoginStatus());
        String scenario = normalizeScenario(request.getScenario());
        long startedAt = System.currentTimeMillis();

        log.info("开始同步聊天, sessionId={}, model={}, userId={}, loginStatus={}, scenario={}, tools={}, messageLength={}",
                request.getSessionId(), modelName, maskedUserId, loginStatus, scenario, toolNames,
                request.getMessage() == null ? 0 : request.getMessage().length());

        ToolRequestContextHolder.set(request);
        try {
            var prompt = chatClient.prompt()
                    .user(augmentedMessage)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.getSessionId()));

            String content = toolNames.isEmpty()
                    ? prompt.call().content()
                    : prompt.toolNames(toolNames.toArray(String[]::new)).call().content();

            log.info("同步聊天完成, sessionId={}, model={}, userId={}, loginStatus={}, scenario={}, tools={}, durationMs={}",
                    request.getSessionId(), modelName, maskedUserId, loginStatus, scenario, toolNames,
                    System.currentTimeMillis() - startedAt);
            return content;
        } catch (Exception ex) {
            log.error("同步聊天失败, sessionId={}, model={}, userId={}, loginStatus={}, scenario={}, tools={}, durationMs={}",
                    request.getSessionId(), modelName, maskedUserId, loginStatus, scenario, toolNames,
                    System.currentTimeMillis() - startedAt, ex);
            throw ex;
        } finally {
            ToolRequestContextHolder.clear();
        }
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
                            .topK(RAG_TOP_K)
                            .similarityThreshold(RAG_SIMILARITY_THRESHOLD)
                            .build());

            if (results == null || results.isEmpty()) {
                log.info("RAG检索完成, hitCount=0, thresholdHit=false, topK={}, threshold={}, queryLength={}",
                        RAG_TOP_K, RAG_SIMILARITY_THRESHOLD, userMessage == null ? 0 : userMessage.length());
                return userMessage;
            }

            List<String> sourceList = results.stream()
                    .map(this::formatSource)
                    .distinct()
                    .toList();
            String context = results.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n---\n\n"));
            String sources = String.join("、", sourceList);

            log.info("RAG检索命中, hitCount={}, thresholdHit=true, topK={}, threshold={}, sources={}",
                    results.size(), RAG_TOP_K, RAG_SIMILARITY_THRESHOLD, sourceList);

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

    List<String> resolveToolNames(ChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return List.of();
        }

        String message = request.getMessage().toLowerCase(Locale.ROOT);
        Set<String> selectedTools = new LinkedHashSet<>();

        if (containsAny(message, "订单", "支付", "退款", "售后")) {
            selectedTools.add(TOOL_ORDER_QUERY);
        }
        if (containsAny(message, "物流", "快递", "配送", "到货", "发货", "运单")) {
            selectedTools.add(TOOL_LOGISTICS_QUERY);
            selectedTools.add(TOOL_ORDER_QUERY);
        }
        if (containsAny(message, "商品", "产品", "奶粉", "纸尿裤", "库存", "规格", "价格", "推荐")) {
            selectedTools.add(TOOL_PRODUCT_QUERY);
        }
        if (containsAny(message, "优惠券", "优惠码", "满减券", "折扣券", "红包")) {
            selectedTools.add(TOOL_COUPON_QUERY);
        }

        return selectedTools.stream()
                .filter(toolName -> {
                    boolean allowed = isToolAllowed(toolName, request);
                    if (!allowed) {
                        logToolDenied(toolName, request);
                    }
                    return allowed;
                })
                .toList();
    }

    String formatSource(Document document) {
        Object source = document.getMetadata().get("source");
        if (source == null) {
            return "未知来源";
        }
        String value = source.toString().trim();
        return value.isEmpty() ? "未知来源" : value;
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isToolAllowed(String toolName, ChatRequest request) {
        String scenario = normalizeScenario(request == null ? null : request.getScenario());
        return switch (toolName) {
            case TOOL_ORDER_QUERY, TOOL_LOGISTICS_QUERY ->
                isLoggedIn(request) && hasUserId(request) && ORDER_TOOL_SCENARIOS.contains(scenario);
            case TOOL_COUPON_QUERY ->
                isLoggedIn(request) && hasUserId(request) && COUPON_TOOL_SCENARIOS.contains(scenario);
            default -> true;
        };
    }

    private void logToolDenied(String toolName, ChatRequest request) {
        log.info("工具权限未通过, sessionId={}, tool={}, userId={}, loginStatus={}, scenario={}",
                request == null ? "" : request.getSessionId(),
                toolName,
                maskUserId(request == null ? null : request.getUserId()),
                normalizeLoginStatus(request == null ? null : request.getLoginStatus()),
                normalizeScenario(request == null ? null : request.getScenario()));
    }

    private boolean hasUserId(ChatRequest request) {
        return request != null && request.getUserId() != null && !request.getUserId().isBlank();
    }

    private boolean isLoggedIn(ChatRequest request) {
        String loginStatus = normalizeLoginStatus(request == null ? null : request.getLoginStatus());
        return "LOGGED_IN".equals(loginStatus)
                || "AUTHENTICATED".equals(loginStatus)
                || "SIGNED_IN".equals(loginStatus)
                || "LOGIN".equals(loginStatus);
    }

    private String normalizeModelName(String modelName) {
        return modelName == null || modelName.isBlank() ? "deepseek" : modelName;
    }

    private String normalizeLoginStatus(String loginStatus) {
        return normalizeUpper(loginStatus, "ANONYMOUS");
    }

    private String normalizeScenario(String scenario) {
        return normalizeUpper(scenario, "GENERAL_ASSISTANT");
    }

    private String normalizeUpper(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String maskUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "anonymous";
        }
        String normalized = userId.trim();
        if (normalized.length() <= 2) {
            return normalized.charAt(0) + "***";
        }
        return normalized.substring(0, 2) + "***" + normalized.substring(normalized.length() - 2);
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
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
