package com.nexusfuture.currency.ai.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfuture.currency.client.PythonSearchClient;
import com.nexusfuture.currency.client.TongyiQwenClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final PythonSearchClient pythonSearchClient;
    private final TongyiQwenClient tongyiQwenClient;
    private final ObjectMapper objectMapper; // 🔥 改为由 Spring 注入

    public String answerQuestion(String question) {
        log.info("📝 [RAG] 收到用户问题: {}", question);

        // 1. 检索相关文档片段
        List<PythonSearchClient.SearchResult> chunks = pythonSearchClient.search(question, 5);

        // 🔥 打印原始检索结果
        try {
            log.info("🔍 [Python 返回的原始数据]: {}", objectMapper.writeValueAsString(chunks));
        } catch (Exception e) {
            log.warn("打印检索结果失败", e);
        }

        if (chunks.isEmpty()) {
            log.warn("⚠️ [RAG] 未检索到任何相关文档片段！");
            return "抱歉，没有在知识库中找到相关的文档信息来回答您的问题。";
        }

        log.info("📚 [RAG] 成功检索到 {} 个相关片段:", chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            PythonSearchClient.SearchResult chunk = chunks.get(i);
            String preview = chunk.getContent().length() > 100 
                ? chunk.getContent().substring(0, 100) + "..." 
                : chunk.getContent();
            
            log.info("   📄 片段 {}: [{}] P{} | 相似度: {} | 内容预览: {}", 
                i + 1, 
                chunk.getDocumentName(), 
                chunk.getPage(),
                chunk.getSimilarity(),
                preview
            );
        }

        // 2. 组装 Prompt
        String context = chunks.stream()
                .map(chunk -> String.format("[来源: %s, 页码: %d] %s", 
                    chunk.getDocumentName(), 
                    chunk.getPage() != null ? chunk.getPage() : 0,
                    chunk.getContent()))
                .collect(Collectors.joining("\n\n"));

        String prompt = String.format(
                "你是一个外汇业务专家。请基于以下参考资料回答问题。\n" +
                "注意：参考资料中可能存在少量OCR识别错误（如缺字），请结合语境智能修复并理解。\n" +
                "如果资料中没有相关信息，请直接回答“知识库中未找到相关答案”。\n\n" +
                "参考资料：\n%s\n\n" +
                "问题：%s",
                context, question
        );

        log.info("🤖 [RAG] 准备将 Prompt 发送给通义千问 LLM...");

        // 3. 调用 LLM 生成最终答案
        String answer = tongyiQwenClient.getChatCompletion(prompt);
        
        log.info("✨ [RAG] LLM 返回最终答案: {}", answer);
        return answer;
    }
}
