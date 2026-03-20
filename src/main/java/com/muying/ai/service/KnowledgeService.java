package com.muying.ai.service;

import com.muying.ai.model.KnowledgeDocument;
import com.muying.ai.rag.EmbeddingPipeline;
import com.muying.ai.KnowledgeDocumentRepository;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class KnowledgeService {

    private final EmbeddingPipeline embeddingPipeline;
    private final VectorStore vectorStore;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public KnowledgeService(EmbeddingPipeline embeddingPipeline,
            VectorStore vectorStore,
            KnowledgeDocumentRepository knowledgeDocumentRepository) {
        this.embeddingPipeline = embeddingPipeline;
        this.vectorStore = vectorStore;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
    }

    public KnowledgeDocument uploadDocument(MultipartFile file, String category) throws IOException {
        return uploadDocument(file.getInputStream(), file.getOriginalFilename(), category);
    }

    public KnowledgeDocument uploadDocument(InputStream inputStream, String fileName, String category) throws IOException {
        String documentId = UUID.randomUUID().toString();

        List<TextSegment> segments = embeddingPipeline.parseAndSplit(inputStream, fileName);

        List<Document> documents = new ArrayList<>();
        for (TextSegment segment : segments) {
            documents.add(new Document(segment.text(), Map.of(
                    "source", fileName,
                    "category", category,
                    "docId", documentId)));
        }
        vectorStore.add(documents);

        KnowledgeDocument metadata = new KnowledgeDocument();
        metadata.setId(documentId);
        metadata.setFileName(fileName);
        metadata.setCategory(category);
        metadata.setSegmentCount(segments.size());
        metadata.setCreatedAt(System.currentTimeMillis());
        knowledgeDocumentRepository.save(metadata);

        log.info("Knowledge document uploaded: {} ({})", fileName, category);
        return metadata;
    }

    public List<KnowledgeDocument> listDocuments() {
        return knowledgeDocumentRepository.findAll().stream()
                .sorted(Comparator.comparingLong(KnowledgeDocument::getCreatedAt).reversed())
                .toList();
    }

    public void deleteDocument(String docId) {
        knowledgeDocumentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge document not found: " + docId));

        vectorStore.delete(new FilterExpressionBuilder()
                .eq("docId", docId)
                .build());
        knowledgeDocumentRepository.deleteById(docId);
        log.info("Knowledge document deleted: {}", docId);
    }
}
