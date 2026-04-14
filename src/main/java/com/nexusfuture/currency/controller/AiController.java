package com.nexusfuture.currency.controller;

import com.nexusfuture.currency.client.TongyiQwenClient;
import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.dto.ai.AiChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController {

    private final TongyiQwenClient tongyiQwenClient;

    /**
     * AI 聊天接口。
     *
     * @param request 包含用户 prompt 的请求体。
     * @return 包含AI生成文本的 Result 对象。
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody AiChatRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            return Result.fail(400, "Prompt不能为空。");
        }

        try {
            String completion = tongyiQwenClient.getChatCompletion(request.getPrompt());
            return Result.success(completion);
        } catch (Exception e) {
            log.error("AI chat failed: {}", e.getMessage());
            // 返回一个对用户友好的错误信息
            return Result.fail(500, "调用AI服务时发生内部错误。");
        }
    }
}
