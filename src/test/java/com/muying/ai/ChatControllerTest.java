package com.muying.ai;

import com.muying.ai.controller.ChatController;
import com.muying.ai.service.ChatService;
import com.muying.ai.service.ModelRouterService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @MockBean
    private ModelRouterService modelRouterService;

    @Test
    void streamChatAcceptsPostJsonAndReturnsEventStream() throws Exception {
        given(chatService.streamChat(ArgumentMatchers.any())).willReturn(Flux.just("你好"));
        given(modelRouterService.getAvailableModels()).willReturn(Set.of("deepseek"));

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "宝宝奶粉怎么冲泡？",
                                  "sessionId": "session-1",
                                  "model": "deepseek"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:message")))
                .andExpect(content().string(containsString("data:")))
                .andExpect(content().string(containsString("event:done")));
    }

    @Test
    void chatGeneratesSessionIdWhenMissing() throws Exception {
        given(chatService.chat(ArgumentMatchers.any())).willReturn("已为你查询到结果");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "查一下优惠券",
                                  "model": "deepseek"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("已为你查询到结果"))
                .andExpect(jsonPath("$.sessionId", not("")));
    }

    @Test
    void chatReturnsBadRequestWhenMessageIsBlank() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "",
                                  "model": "deepseek"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatReturnsBadRequestWhenMessageIsMissing() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "model": "deepseek"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getModelsReturnsAvailableModelNames() throws Exception {
        given(modelRouterService.getAvailableModels()).willReturn(Set.of("deepseek", "qianwen", "zhipu"));

        mockMvc.perform(get("/api/chat/models"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
