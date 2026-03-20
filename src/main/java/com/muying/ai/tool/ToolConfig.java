package com.muying.ai.tool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * Function Calling 工具注册
 * 使用 Spring AI 推荐方式：@Bean + @Description 注册 Function
 * AI 会根据用户意图自动选择调用对应工具
 */
@Configuration
public class ToolConfig {

    @Bean
    @Description("根据订单ID查询订单状态，包括订单详情、支付状态、发货状态等")
    public Function<OrderQueryTool.Request, OrderQueryTool.Response> orderQuery(OrderQueryTool tool) {
        return tool;
    }

    @Bean
    @Description("根据商品ID查询商品信息，包括名称、价格、库存、规格等")
    public Function<ProductQueryTool.Request, ProductQueryTool.Response> productQuery(ProductQueryTool tool) {
        return tool;
    }

    @Bean
    @Description("根据订单ID查询物流信息，包括快递公司、运单号、物流轨迹等")
    public Function<LogisticsQueryTool.Request, LogisticsQueryTool.Response> logisticsQuery(LogisticsQueryTool tool) {
        return tool;
    }

    @Bean
    @Description("根据用户ID查询可用优惠券列表，包括优惠金额、使用条件、有效期等")
    public Function<CouponQueryTool.Request, CouponQueryTool.Response> couponQuery(CouponQueryTool tool) {
        return tool;
    }
}
