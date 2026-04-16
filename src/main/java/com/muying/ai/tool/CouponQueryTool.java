package com.muying.ai.tool;

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

    public CouponQueryTool(RestClient mallRestClient) {
        this.restClient = mallRestClient;
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
            return new Response("查询优惠券失败，请稍后重试");
        }
    }

    public record Request(String userId) {}
    public record Response(String result) {}
}
