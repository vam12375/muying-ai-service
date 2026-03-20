package com.muying.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muying.ai.RedisChatMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisChatMemoryRepositoryTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private RedisChatMemoryRepository repository;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        repository = new RedisChatMemoryRepository(stringRedisTemplate, new ObjectMapper());
    }

    @Test
    void saveAllStoresConversationAndTracksConversationId() {
        List<Message> messages = List.of(new UserMessage("你好"), new AssistantMessage("您好"));

        repository.saveAll("session-1", messages);

        verify(setOperations).add(RedisChatMemoryRepository.CONVERSATION_IDS_KEY, "session-1");
        verify(valueOperations).set(eq(RedisChatMemoryRepository.conversationKey("session-1")),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void findByConversationIdRestoresSavedMessages() {
        List<Message> messages = List.of(new UserMessage("你好"), new AssistantMessage("您好"));
        repository.saveAll("session-1", messages);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(RedisChatMemoryRepository.conversationKey("session-1")),
                payloadCaptor.capture());
        when(valueOperations.get(RedisChatMemoryRepository.conversationKey("session-1")))
                .thenReturn(payloadCaptor.getValue());

        List<Message> restored = repository.findByConversationId("session-1");

        assertThat(restored).hasSize(2);
        assertThat(restored.get(0)).isInstanceOf(UserMessage.class);
        assertThat(restored.get(0).getText()).isEqualTo("你好");
        assertThat(restored.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(restored.get(1).getText()).isEqualTo("您好");
    }

    @Test
    void deleteByConversationIdRemovesStoredConversationAndIndex() {
        repository.deleteByConversationId("session-1");

        verify(stringRedisTemplate).delete(RedisChatMemoryRepository.conversationKey("session-1"));
        verify(setOperations).remove(RedisChatMemoryRepository.CONVERSATION_IDS_KEY, "session-1");
    }

    @Test
    void findConversationIdsReturnsKnownIds() {
        when(setOperations.members(RedisChatMemoryRepository.CONVERSATION_IDS_KEY))
                .thenReturn(Set.of("session-2", "session-1"));

        assertThat(repository.findConversationIds()).containsExactlyInAnyOrder("session-1", "session-2");
    }
}
