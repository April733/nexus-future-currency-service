package com.nexusfuture.currency;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AiControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    // ❌ 你原来的流式测试（保留不动）
    @Test
    void testChatStream() {
        webTestClient.get().uri("/api/ai/chat-stream?prompt=对比吉隆坡和曼谷的天气？")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM)
                .returnResult(String.class)
                .getResponseBody()
                .doOnNext(data -> {
                    System.out.println("前端收到的流式数据：" + data);
                })
                .blockLast();
    }

    // =====================================================================
    // ✅【新增】真正后端用的：拿到完整字符串、可打印、可存库、可记录日志
    // =====================================================================
    @Test
    void testChat_Normal_Backend_Interface() {
        // 后端接口 → 返回完整文本，不是流
        String fullAnswer = webTestClient.get()
                .uri("/api/ai/chat?prompt=对比吉隆坡和曼谷的天气？")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .single()
                .block();

        // 打印完整结果
        System.out.println("\n===== 【后端完整返回结果】 =====");
        System.out.println(fullAnswer);
        System.out.println("====================================\n");

        // 断言一定不为空（100% 成功）
        assertThat(fullAnswer).isNotBlank();
        assertThat(fullAnswer).contains("吉隆坡", "曼谷", "天气");
    }
}