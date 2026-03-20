package com.muying.ai.config;

import com.muying.ai.RedisChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemoryRepository chatMemoryRepository(RedisChatMemoryRepository redisChatMemoryRepository) {
        return redisChatMemoryRepository;
    }
}
