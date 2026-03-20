package com.muying.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 多模型属性配置
 * 从 application.yml 中读取 ai.models 下的所有模型配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiModelProperties {

    private Map<String, ModelConfig> models;

    @Data
    public static class ModelConfig {
        /** API 密钥 */
        private String apiKey;
        /** 模型 API 地址（OpenAI 兼容格式） */
        private String baseUrl;
        /** 模型名称 */
        private String model;
    }
}
