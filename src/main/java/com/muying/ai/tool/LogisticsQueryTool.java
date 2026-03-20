package com.muying.ai.tool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

/**
 * 物流查询工具
 * AI 通过 Function Calling 自动调用，查询母婴商城的物流进度
 */
@Component
public class LogisticsQueryTool implements Function<LogisticsQueryTool.Request, LogisticsQueryTool.Response> {

    private final RestClient restClient;

    public LogisticsQueryTool(@Value("${mall.api.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Response apply(Request request) {
        try {
            String result = restClient.get()
                    .uri("/logistics/order/{orderId}", request.orderId())
                    .retrieve()
                    .body(String.class);
            return new Response(result);
        } catch (Exception e) {
            return new Response("查询物流失败：" + e.getMessage());
        }
    }

    public record Request(String orderId) {}
    public record Response(String result) {}
}
