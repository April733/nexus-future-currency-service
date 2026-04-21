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
@SpringBootTest // 🔥 启动完整容器，加载所有真实 Bean (Python Client, LLM Client 等)
@AutoConfigureMockMvc
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("🔥 真实链路: 基础版 RAG 问答")
    void testRealRagAsk() throws Exception {
        log.info("--------------------------------------------------");
        log.info("🚀 开始执行【真实】集成测试 - Ask Basic");
        log.info("⚠️ 请确保 Python 服务 (localhost:8000) 已启动！");
        log.info("--------------------------------------------------");

        String question = "个人在印度尼西亚的外汇额度是多少？";
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
    @DisplayName("🔥 真实链路: 增强版 RAG 问答 (带溯源)")
    void testRealRagAskPlus() throws Exception {
        log.info("--------------------------------------------------");
        log.info("🚀 开始执行【真实】集成测试 - Ask Plus");
        
        String question = "个人在印度尼西亚的外汇额度是多少？";
        log.info("📤 发送真实请求, 入参: {}", question);

        mockMvc.perform(post("/api/rag/ask-plus")
                        .param("question", question))
                .andDo(print()) // 🔥 打印包含 citations 的真实 JSON
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 验证返回的是结构化对象
                .andExpect(jsonPath("$.data.answer").exists())
                // 验证包含 citations 数组
                .andExpect(jsonPath("$.data.citations").isArray());
        
        log.info("✅ 增强版真实链路测试通过！");
        log.info("--------------------------------------------------");
    }

    @Test
    @DisplayName("❌ 真实链路: 空参数校验")
    void testRealRagEmptyParam() throws Exception {
        log.info("📤 发送真实请求, 入参: [空字符串]");
        
        mockMvc.perform(post("/api/rag/ask")
                        .param("question", ""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
