package com.nexusfuture.currency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfuture.currency.client.TongyiQwenClient;
import com.nexusfuture.currency.controller.AiController;
import com.nexusfuture.currency.dto.ai.AiChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
public class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TongyiQwenClient tongyiQwenClient;

    @Test
    void testChat_Success() throws Exception {
        // 1. 准备 (Arrange)
        String userPrompt = "你好，请介绍一下你自己。";
        String expectedAiResponse = "你好！我是一个由通义千问强力驱动的AI助手。";
        AiChatRequest request = new AiChatRequest();
        request.setPrompt(userPrompt);

        // 当客户端的 getChatCompletion 方法被以任何字符串调用时，都返回我们预设的回复
        when(tongyiQwenClient.getChatCompletion(anyString())).thenReturn(expectedAiResponse);

        // 2. 执行 (Act) & 3. 断言 (Assert)
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print()) // 打印详细的请求和响应信息以供调试
                .andExpect(status().isOk()) // 验证HTTP状态码为200
                .andExpect(jsonPath("$.code").value(200)) // 验证我们自定义的业务码为200
                .andExpect(jsonPath("$.data").value(expectedAiResponse)); // 验证返回的数据是AI的回复
    }

    @Test
    void testChat_EmptyPrompt() throws Exception {
        // 1. 准备 (Arrange)
        AiChatRequest request = new AiChatRequest();
        request.setPrompt(""); // 空的 prompt

        // 2. 执行 (Act) & 3. 断言 (Assert)
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk()) // 我们的控制器逻辑返回的是业务失败，HTTP状态码仍为200
                .andExpect(jsonPath("$.code").value(400)) // 验证业务码为400
                .andExpect(jsonPath("$.msg").value("Prompt不能为空。")); // 验证错误信息
    }

    @Test
    void testChat_ServiceThrowsException() throws Exception {
        // 1. 准备 (Arrange)
        AiChatRequest request = new AiChatRequest();
        request.setPrompt("这是一个会触发错误的prompt");

        // 当客户端方法被调用时，模拟抛出一个运行时异常
        when(tongyiQwenClient.getChatCompletion(anyString())).thenThrow(new RuntimeException("网络连接失败"));

        // 2. 执行 (Act) & 3. 断言 (Assert)
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500)) // 验证业务码为500
                .andExpect(jsonPath("$.msg").value("调用AI服务时发生内部错误。")); // 验证对用户友好的错误信息
    }
}
