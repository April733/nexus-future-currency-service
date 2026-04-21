package com.nexusfuture.currency;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class ToolGatewayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("🔥 真实链路: 通过网关执行汇率查询工具")
    void testExecuteExchangeRateTool() throws Exception {
        log.info("--------------------------------------------------");
        log.info("🚀 开始执行【真实】工具网关集成测试");
        
        // 注意：argumentsJson 需要是一个转义后的 JSON 字符串
        String jsonRequest = """
                {
                    "toolName": "get_exchange_rate",
                    "argumentsJson": "{\\"fromCurrency\\": \\"USD\\", \\"toCurrency\\": \\"CNY\\"}"
                }
                """;

        log.info("📤 发送工具执行请求: {}", jsonRequest);

        mockMvc.perform(post("/api/tool/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print()) 
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 🔥 修改点：doesNotHaveJsonPath() 不需要传参，它直接作用于前面的 jsonPath
                .andExpect(jsonPath("$.data.error").doesNotHaveJsonPath())
                // 验证返回的字符串里包含汇率关键字
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("rate")));
        
        log.info("✅ 工具网关真实链路测试通过！");
        log.info("--------------------------------------------------");
    }

    @Test
    @DisplayName("❌ 真实链路: 调用不存在的工具")
    void testExecuteNonExistentTool() throws Exception {
        String jsonRequest = """
                {
                    "toolName": "fake_tool_name",
                    "arguments": {}
                }
                """;

        mockMvc.perform(post("/api/tool/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
