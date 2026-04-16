package com.muying.ai.service;

import com.muying.ai.model.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ModelRouterService modelRouterService;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatMemoryRepository chatMemoryRepository;

    @Mock
    private RagAlertNotifier ragAlertNotifier;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(modelRouterService, vectorStore, chatMemoryRepository, ragAlertNotifier);
    }

    @Test
    void buildAugmentedMessageReturnsOriginalMessageWhenNoRagResult() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        String result = chatService.buildAugmentedMessage("宝宝奶粉怎么冲泡？");

        assertThat(result).isEqualTo("宝宝奶粉怎么冲泡？");
    }

    @Test
    void buildAugmentedMessageIncludesContextAndSourcesWhenRagResultExists() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("第一段知识", Map.of("source", "奶粉冲泡指南.md")),
                new Document("第二段知识", Map.of("source", "新生儿护理.md")),
                new Document("重复来源知识", Map.of("source", "奶粉冲泡指南.md"))));

        String result = chatService.buildAugmentedMessage("宝宝奶粉怎么冲泡？");

        assertThat(result)
                .contains("用户问题：宝宝奶粉怎么冲泡？")
                .contains("参考资料来源：奶粉冲泡指南.md、新生儿护理.md")
                .contains("第一段知识")
                .contains("第二段知识")
                .contains("重复来源知识");
    }

    @Test
    void buildAugmentedMessageFallsBackWhenVectorStoreThrowsException() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new IllegalStateException("milvus unavailable"));

        String result = chatService.buildAugmentedMessage("物流什么时候到？");

        assertThat(result).isEqualTo("物流什么时候到？");
    }

    @Test
    void formatSourceReturnsUnknownWhenSourceMetadataMissing() {
        String source = chatService.formatSource(new Document("内容", Map.of()));

        assertThat(source).isEqualTo("未知来源");
    }

    @Test
    void buildAugmentedMessageUsesOriginalQueryForSimilaritySearch() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        ChatRequest request = new ChatRequest();
        request.setMessage("查询会员权益");

        chatService.buildAugmentedMessage(request.getMessage());

        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void resolveToolNamesReturnsOrderAndLogisticsToolsForDeliveryQuestionWhenUserIsAuthorized() {
        ChatRequest request = new ChatRequest();
        request.setMessage("订单物流什么时候到？");
        request.setUserId("user-10001");
        request.setLoginStatus("LOGGED_IN");
        request.setScenario("ORDER_SERVICE");

        List<String> tools = chatService.resolveToolNames(request);

        assertThat(tools).containsExactly("orderQuery", "logisticsQuery");
    }

    @Test
    void resolveToolNamesSkipsRestrictedToolsForAnonymousDeliveryQuestion() {
        ChatRequest request = new ChatRequest();
        request.setMessage("订单物流什么时候到？");

        List<String> tools = chatService.resolveToolNames(request);

        assertThat(tools).isEmpty();
    }

    @Test
    void resolveToolNamesReturnsProductAndCouponToolsForPromotionQuestionWhenUserIsAuthorized() {
        ChatRequest request = new ChatRequest();
        request.setMessage("这款奶粉价格多少，有优惠券吗？");
        request.setUserId("user-10001");
        request.setLoginStatus("LOGGED_IN");
        request.setScenario("PROMOTION_SERVICE");

        List<String> tools = chatService.resolveToolNames(request);

        assertThat(tools).containsExactly("productQuery", "couponQuery");
    }

    @Test
    void resolveToolNamesSkipsCouponToolWhenPromotionQuestionUserIsAnonymous() {
        ChatRequest request = new ChatRequest();
        request.setMessage("这款奶粉价格多少，有优惠券吗？");

        List<String> tools = chatService.resolveToolNames(request);

        assertThat(tools).containsExactly("productQuery");
    }

    @Test
    void resolveToolNamesReturnsEmptyWhenQuestionDoesNotNeedTools() {
        ChatRequest request = new ChatRequest();
        request.setMessage("新生儿洗澡需要注意什么？");

        List<String> tools = chatService.resolveToolNames(request);

        assertThat(tools).isEmpty();
    }
}
