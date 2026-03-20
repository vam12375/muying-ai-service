package com.muying.ai;

import com.muying.ai.model.KnowledgeDocument;
import com.muying.ai.rag.EmbeddingPipeline;
import com.muying.ai.service.KnowledgeService;
import dev.langchain4j.data.segment.TextSegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.converter.PrintFilterExpressionConverter;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private EmbeddingPipeline embeddingPipeline;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @InjectMocks
    private KnowledgeService knowledgeService;

    @Test
    void uploadDocumentPersistsMetadataThroughRepository() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "guide.md",
                "text/markdown",
                "奶粉冲泡说明".getBytes()
        );
        when(embeddingPipeline.parseAndSplit(any(), any())).thenReturn(List.of(TextSegment.from("分段内容")));

        KnowledgeDocument result = knowledgeService.uploadDocument(file, "faq");

        ArgumentCaptor<KnowledgeDocument> metadataCaptor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> documentCaptor = ArgumentCaptor.forClass((Class<List<Document>>) (Class<?>) List.class);
        verify(vectorStore).add(documentCaptor.capture());
        verify(knowledgeDocumentRepository).save(metadataCaptor.capture());

        KnowledgeDocument saved = metadataCaptor.getValue();
        Document storedSegment = documentCaptor.getValue().getFirst();
        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(saved.getFileName()).isEqualTo("guide.md");
        assertThat(saved.getCategory()).isEqualTo("faq");
        assertThat(storedSegment.getMetadata()).containsEntry("docId", saved.getId());
    }

    @Test
    void deleteDocumentRemovesVectorsByDocIdFilterBeforeDeletingMetadata() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId("doc-1");
        when(knowledgeDocumentRepository.findById("doc-1")).thenReturn(Optional.of(document));

        knowledgeService.deleteDocument("doc-1");

        ArgumentCaptor<Filter.Expression> filterCaptor = ArgumentCaptor.forClass(Filter.Expression.class);
        verify(vectorStore).delete(filterCaptor.capture());
        verify(knowledgeDocumentRepository).deleteById("doc-1");

        String rendered = new PrintFilterExpressionConverter().convertExpression(filterCaptor.getValue());
        assertThat(rendered).contains("docId").contains("doc-1");
    }

    @Test
    void deleteDocumentKeepsMetadataWhenVectorDeletionFails() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId("doc-1");
        when(knowledgeDocumentRepository.findById("doc-1")).thenReturn(Optional.of(document));
        doThrow(new IllegalStateException("milvus unavailable")).when(vectorStore).delete(any(Filter.Expression.class));

        assertThatThrownBy(() -> knowledgeService.deleteDocument("doc-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("milvus unavailable");
    }
}
