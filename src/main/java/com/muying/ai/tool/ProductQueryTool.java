package com.muying.ai.tool;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

/**
 * 商品查询工具
 * AI 通过 Function Calling 自动调用，查询母婴商城的商品信息
 */
@Component
public class ProductQueryTool implements Function<ProductQueryTool.Request, ProductQueryTool.Response> {

    private final RestClient restClient;

    public ProductQueryTool(RestClient mallRestClient) {
        this.restClient = mallRestClient;
    }

    @Override
    public Response apply(Request request) {
        try {
            String result = restClient.get()
                    .uri("/product/{productId}", request.productId())
                    .retrieve()
                    .body(String.class);
            return new Response(result);
        } catch (Exception e) {
            return new Response("查询商品失败，请稍后重试");
        }
    }

    public record Request(String productId) {}
    public record Response(String result) {}
}
