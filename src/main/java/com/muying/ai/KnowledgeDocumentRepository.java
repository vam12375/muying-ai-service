package com.muying.ai;

import com.muying.ai.model.KnowledgeDocument;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentRepository {

    void save(KnowledgeDocument document);

    List<KnowledgeDocument> findAll();

    Optional<KnowledgeDocument> findById(String id);

    void deleteById(String id);
}
