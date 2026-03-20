package com.muying.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 3 文档配置
 * 访问地址：http://localhost:8090/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("母婴商城 AI 智能客服 API")
                        .description("基于 Spring AI + LangChain4j 的 RAG + Function Calling 智能客服系统。\n\n" +
                                "**主要功能：**\n" +
                                "- 流式 SSE 对话 / 同步对话\n" +
                                "- 多模型路由（DeepSeek / 通义千问 / 智谱）\n" +
                                "- RAG 知识库检索增强\n" +
                                "- Function Calling（订单/物流/商品/优惠券查询）\n" +
                                "- 对话记忆持久化（Redis）")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("muying-ai-service")
                                .email("dev@muying.com"))
                        .license(new License()
                                .name("MIT")));
    }
}
