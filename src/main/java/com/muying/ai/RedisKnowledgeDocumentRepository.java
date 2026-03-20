package com.muying.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muying.ai.model.KnowledgeDocument;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RedisKnowledgeDocumentRepository implements com.muying.ai.KnowledgeDocumentRepository {

    static final String KNOWLEDGE_DOCUMENT_KEY = "ai:knowledge:documents";

    private final HashOperations<String, String, String> hashOperations;
    private final ObjectMapper objectMapper;

    public RedisKnowledgeDocumentRepository(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.hashOperations = stringRedisTemplate.opsForHash();
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(KnowledgeDocument document) {
        hashOperations.put(KNOWLEDGE_DOCUMENT_KEY, document.getId(), write(document));
    }

    @Override
    public List<KnowledgeDocument> findAll() {
        return hashOperations.entries(KNOWLEDGE_DOCUMENT_KEY)
                .values()
                .stream()
                .map(this::read)
                .toList();
    }

    @Override
    public Optional<KnowledgeDocument> findById(String id) {
        return Optional.ofNullable(hashOperations.get(KNOWLEDGE_DOCUMENT_KEY, id))
                .map(this::read);
    }

    @Override
    public void deleteById(String id) {
        hashOperations.delete(KNOWLEDGE_DOCUMENT_KEY, id);
    }

    private String write(KnowledgeDocument document) {
        try {
            return objectMapper.writeValueAsString(document);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize knowledge document", exception);
        }
    }

    private KnowledgeDocument read(String value) {
        try {
            return objectMapper.readValue(value, KnowledgeDocument.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize knowledge document", exception);
        }
    }
}
