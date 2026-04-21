package com.nexusfuture.currency.ai.controller;

import com.nexusfuture.currency.ai.rag.service.RagService;
import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.dto.ai.RagAnswerDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * RAG 问答接口
     */
    @PostMapping("/ask")
    public Result<String> askQuestion(@RequestParam String question) {
        log.info("--------------------------------------------------");
        log.info("   - askQuestion 入参 (question): {}", question);

        if (question == null || question.trim().isEmpty()) {
            log.warn("   - 校验失败: 问题为空");
            return Result.fail(400, "问题不能为空");
        }
        
        try {
            String answer = ragService.answerQuestion(question);
            
            Result<String> result = Result.success(answer);
            log.info("📤 [API 响应] 处理成功");
            log.info("   - 出参 (answer): {}", answer);
            log.info("--------------------------------------------------");
            return result;
        } catch (Exception e) {
            log.error("❌ [API 异常] RAG 问答失败: {}", e.getMessage(), e);
            log.info("--------------------------------------------------");
            return Result.fail(500, "服务繁忙，请稍后再试");
        }
    }

    /**
     * 【增量接口】带引用溯源的 RAG 问答
     */
    @PostMapping("/ask-plus")
    public Result<RagAnswerDto> askQuestionPlus(@RequestParam String question) {
        if (question == null || question.trim().isEmpty()) {
            return Result.fail(400, "问题不能为空");
        }
        
        try {
            RagAnswerDto answerDto = ragService.answerWithCitations(question);
            return Result.success(answerDto);
        } catch (Exception e) {
            log.error("RAG 增强问答失败: {}", e.getMessage(), e);
            return Result.fail(500, "服务繁忙，请稍后再试");
        }
    }
}
