package com.nexusfuture.currency;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest // 🔥 启动完整的 Spring Boot 容器，加载所有真实的 Bean
@AutoConfigureMockMvc
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("🔥 真实链路测试: 调用 Python 检索并返回结果")
    void testRealRagAsk() throws Exception {
        log.info("--------------------------------------------------");
        log.info("🚀 开始执行【真实】集成测试");
        log.info("⚠️ 注意：请确保 Python 服务 (localhost:8000) 已启动！");
        log.info("--------------------------------------------------");

        String question = "个人外汇额度是多少？";
        log.info("📤 发送真实请求, 入参: {}", question);

        mockMvc.perform(post("/api/rag/ask")
                        .param("question", question))
                .andDo(print()) // 🔥 打印真实的 HTTP 响应
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        
        log.info("✅ 真实链路测试通过！");
        log.info("--------------------------------------------------");
    }

    @Test
    @DisplayName("❌ 真实链路测试: 空参数校验")
    void testRealRagEmptyParam() throws Exception {
        log.info("📤 发送真实请求, 入参: [空字符串]");
        
        mockMvc.perform(post("/api/rag/ask")
                        .param("question", ""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
