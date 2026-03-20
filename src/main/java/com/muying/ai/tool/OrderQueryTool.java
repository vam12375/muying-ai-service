package com.muying.ai.tool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

/**
 * 订单查询工具
 * AI 通过 Function Calling 自动调用，查询母婴商城的订单状态
 */
@Component
public class OrderQueryTool implements Function<OrderQueryTool.Request, OrderQueryTool.Response> {

    private final RestClient restClient;

    public OrderQueryTool(@Value("${mall.api.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Response apply(Request request) {
        try {
            String result = restClient.get()
                    .uri("/order/{orderId}", request.orderId())
                    .retrieve()
                    .body(String.class);
            return new Response(result);
        } catch (Exception e) {
            return new Response("查询订单失败：" + e.getMessage());
        }
    }

    /** 订单查询请求参数 */
    public record Request(String orderId) {}
    /** 订单查询响应 */
    public record Response(String result) {}
}
