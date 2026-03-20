package com.muying.ai.tool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

/**
 * 优惠券查询工具
 * AI 通过 Function Calling 自动调用，查询用户可用的优惠券列表
 */
@Component
public class CouponQueryTool implements Function<CouponQueryTool.Request, CouponQueryTool.Response> {

    private final RestClient restClient;

    public CouponQueryTool(@Value("${mall.api.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Response apply(Request request) {
        try {
            String result = restClient.get()
                    .uri("/coupon/available?userId={userId}", request.userId())
                    .retrieve()
                    .body(String.class);
            return new Response(result);
        } catch (Exception e) {
            return new Response("查询优惠券失败：" + e.getMessage());
        }
    }

    public record Request(String userId) {}
    public record Response(String result) {}
}
