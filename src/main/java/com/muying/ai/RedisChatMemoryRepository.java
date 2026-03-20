package com.muying.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    public static final String CONVERSATION_IDS_KEY = "ai:chat:memory:conversation-ids";
    private static final String CONVERSATION_KEY_PREFIX = "ai:chat:memory:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ValueOperations<String, String> valueOperations;
    private final SetOperations<String, String> setOperations;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.valueOperations = stringRedisTemplate.opsForValue();
        this.setOperations = stringRedisTemplate.opsForSet();
        this.objectMapper = objectMapper;
    }

    public static String conversationKey(String conversationId) {
        return CONVERSATION_KEY_PREFIX + conversationId;
    }

    @Override
    public List<String> findConversationIds() {
        Set<String> conversationIds = setOperations.members(CONVERSATION_IDS_KEY);
        if (conversationIds == null || conversationIds.isEmpty()) {
            return List.of();
        }
        return conversationIds.stream().sorted().toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String payload = valueOperations.get(conversationKey(conversationId));
        if (payload == null || payload.isBlank()) {
            return List.of();
        }

        try {
            RedisMessageRecord[] records = objectMapper.readValue(payload, RedisMessageRecord[].class);
            return Arrays.stream(records).map(this::toMessage).toList();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize chat memory", exception);
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        try {
            String payload = objectMapper.writeValueAsString(messages.stream().map(this::toRecord).toList());
            valueOperations.set(conversationKey(conversationId), payload);
            setOperations.add(CONVERSATION_IDS_KEY, conversationId);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize chat memory", exception);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        stringRedisTemplate.delete(conversationKey(conversationId));
        setOperations.remove(CONVERSATION_IDS_KEY, conversationId);
    }

    private RedisMessageRecord toRecord(Message message) {
        Map<String, Object> metadata = message instanceof AbstractMessage abstractMessage
                ? abstractMessage.getMetadata()
                : Map.of();

        if (message instanceof UserMessage userMessage) {
            return new RedisMessageRecord("USER", userMessage.getText(), metadata, List.of(), List.of());
        }
        if (message instanceof SystemMessage systemMessage) {
            return new RedisMessageRecord("SYSTEM", systemMessage.getText(), metadata, List.of(), List.of());
        }
        if (message instanceof AssistantMessage assistantMessage) {
            return new RedisMessageRecord(
                    "ASSISTANT",
                    assistantMessage.getText(),
                    metadata,
                    assistantMessage.getToolCalls(),
                    List.of());
        }
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            return new RedisMessageRecord(
                    "TOOL",
                    null,
                    metadata,
                    List.of(),
                    toolResponseMessage.getResponses());
        }
        throw new IllegalArgumentException("Unsupported message type: " + message.getClass().getName());
    }

    private Message toMessage(RedisMessageRecord record) {
        Map<String, Object> metadata = record.metadata() == null ? Map.of() : record.metadata();
        return switch (record.type()) {
            case "USER" -> new UserMessage(record.text());
            case "SYSTEM" -> new SystemMessage(record.text());
            case "ASSISTANT" -> new AssistantMessage(
                    record.text(),
                    metadata,
                    record.toolCalls() == null ? List.of() : record.toolCalls());
            case "TOOL" -> new ToolResponseMessage(
                    record.toolResponses() == null ? List.of() : record.toolResponses(),
                    metadata);
            default -> throw new IllegalArgumentException("Unsupported message type: " + record.type());
        };
    }

    record RedisMessageRecord(
            String type,
            String text,
            Map<String, Object> metadata,
            List<AssistantMessage.ToolCall> toolCalls,
            List<ToolResponseMessage.ToolResponse> toolResponses) {
    }
}
