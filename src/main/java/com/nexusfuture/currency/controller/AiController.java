package com.nexusfuture.currency.controller;

import com.nexusfuture.currency.client.TongyiQwenClient;
import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.dto.ai.AiChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final TongyiQwenClient tongyiQwenClient;

    // 使用一个单线程的线程池来处理SSE的异步任务，避免阻塞主线程
    private final ExecutorService sseExecutor = Executors.newSingleThreadExecutor();

    public AiController(TongyiQwenClient tongyiQwenClient) {
        this.tongyiQwenClient = tongyiQwenClient;
    }

    /**
     * 【保留的非流式聊天接口】
     * 接收一个包含 prompt 的 JSON 对象，并一次性返回完整的 AI 回复。
     */
    @GetMapping("/chat")
    public Result<String> chat(@RequestParam String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return Result.fail(400, "Prompt不能为空。");
        }

        try {
            String aiResponse = tongyiQwenClient.getChatCompletion(prompt);
            return Result.success(aiResponse);
        } catch (Exception e) {
            log.error("调用AI服务时发生错误: {}", e.getMessage(), e);
            return Result.fail(500, "调用AI服务时发生内部错误。");
        }
    }


    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String prompt) {
        return tongyiQwenClient.getChatCompletionStream(prompt);
    }

    /**
     * 【新增的Function Calling接口】
     * 接收一个 prompt，并允许AI在处理过程中调用已注册的工具（如查询汇率）。
     *
     * @param prompt 用户的请求，例如 "查询一下人民币和美元的汇率"
     * @return 包含AI最终答复的Result对象
     */
    @GetMapping("/chat-with-tools")
    public Result<String> chatWithTools(@RequestParam String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return Result.fail(400, "Prompt不能为空。");
        }

        try {
            String aiResponse = tongyiQwenClient.getChatCompletionWithTools(prompt);
            return Result.success(aiResponse);
        } catch (Exception e) {
            log.error("调用带工具的AI服务时发生错误: {}", e.getMessage(), e);
            return Result.fail(500, "调用带工具的AI服务时发生内部错误。");
        }
    }
}