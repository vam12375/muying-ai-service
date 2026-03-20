package com.muying.ai.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 多模型路由服务
 * 根据前端传入的模型名称，通过策略模式返回对应的 ChatModel 实例
 */
@Service
public class ModelRouterService {

    private final Map<String, OpenAiChatModel> chatModelMap;

    public ModelRouterService(Map<String, OpenAiChatModel> chatModelMap) {
        this.chatModelMap = chatModelMap;
    }

    /**
     * 根据模型名称获取对应的 ChatModel
     * 默认返回 deepseek
     */
    public ChatModel getModel(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return chatModelMap.get("deepseek");
        }
        return chatModelMap.getOrDefault(modelName, chatModelMap.get("deepseek"));
    }

    /**
     * 获取所有可用模型名称
     */
    public Set<String> getAvailableModels() {
        return chatModelMap.keySet();
    }
}
