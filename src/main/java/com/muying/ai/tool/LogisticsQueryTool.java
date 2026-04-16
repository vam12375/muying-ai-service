package com.muying.ai.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

/**
 * 物流查询工具
 * AI 通过 Function Calling 自动调用，查询母婴商城的物流进度
 */
@Slf4j
@Component
public class LogisticsQueryTool implements Function<LogisticsQueryTool.Request, LogisticsQueryTool.Response> {

    private static final String[] ALLOWED_SCENARIOS = {
            "GENERAL_ASSISTANT", "ORDER_SERVICE", "LOGISTICS_SERVICE", "AFTER_SALES"
    };

    private final RestClient restClient;

    public LogisticsQueryTool(RestClient mallRestClient) {
        this.restClient = mallRestClient;
    }

    @Override
    public Response apply(Request request) {
        ToolRequestContextHolder.ToolRequestContext context = ToolRequestContextHolder.get();
        if (!context.isLoggedIn() || !context.hasUserId() || !context.matchesScenario(ALLOWED_SCENARIOS)) {
            log.warn("物流工具权限拒绝, userId={}, loginStatus={}, scenario={}, orderId={}",
                    maskUserId(context.userId()), context.loginStatus(), context.scenario(),
                    request == null ? "" : request.orderId());
            return new Response("当前身份或场景无权查询物流，请先登录后重试");
        }
        if (request == null || request.orderId() == null || request.orderId().isBlank()) {
            return new Response("订单号不能为空，请提供有效订单号后重试");
        }

        try {
            String result = restClient.get()
                    .uri("/logistics/order/{orderId}", request.orderId())
                    .retrieve()
                    .body(String.class);
            return new Response(result);
        } catch (Exception e) {
            log.error("查询物流失败, userId={}, scenario={}, orderId={}",
                    maskUserId(context.userId()), context.scenario(), request.orderId(), e);
            return new Response("查询物流失败，请稍后重试");
        }
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

    public record Request(String orderId) {
    }

    public record Response(String result) {
    }
}
