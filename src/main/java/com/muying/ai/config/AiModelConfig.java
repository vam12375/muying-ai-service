package com.muying.ai.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 多模型配置
 * 基于 OpenAI 兼容协议，为每个国产模型（DeepSeek/通义/智谱）创建独立的 ChatModel 实例
 */
@Configuration
public class AiModelConfig {

    @Bean
    public Map<String, OpenAiChatModel> chatModelMap(AiModelProperties properties) {
        Map<String, OpenAiChatModel> models = new HashMap<>();

        properties.getModels().forEach((name, config) -> {
            // 所有国产模型都兼容 OpenAI API 格式，统一用 OpenAiApi 适配
            OpenAiApi api = OpenAiApi.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(config.getBaseUrl())
                    .build();

            OpenAiChatModel model = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(config.getModel())
                            .temperature(0.7)
                            .maxTokens(2048)
                            .build())
                    .build();

            models.put(name, model);
        });

        return models;
    }
}
