package com.muying.ai.service;

import com.muying.ai.model.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ChatService.class)
class ChatServiceIntegrationTest {

    @Autowired
    private ChatService chatService;

    @MockBean
    private ModelRouterService modelRouterService;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private ChatMemoryRepository chatMemoryRepository;

    @MockBean
    private RagAlertNotifier ragAlertNotifier;

    @Test
    void buildAugmentedMessageWorksInsideSpringContext() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("会员权益说明", Map.of("source", "会员权益说明.md"))));

        ChatRequest request = new ChatRequest();
        request.setMessage("最近有什么会员权益？");

        String result = chatService.buildAugmentedMessage(request.getMessage());

        assertThat(result)
                .contains("用户问题：最近有什么会员权益？")
                .contains("参考资料来源：会员权益说明.md")
                .contains("会员权益说明");
    }

    @Test
    void buildAugmentedMessageFallsBackInsideSpringContextWhenVectorStoreReturnsEmpty() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        String result = chatService.buildAugmentedMessage("知识库里有育儿建议吗？");

        assertThat(result).isEqualTo("知识库里有育儿建议吗？");
    }
}
