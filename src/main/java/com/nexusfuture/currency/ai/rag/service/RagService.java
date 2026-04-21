package com.nexusfuture.currency.ai.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfuture.currency.client.PythonSearchClient;
import com.nexusfuture.currency.client.TongyiQwenClient;
import com.nexusfuture.currency.constant.PromptConstants;
import com.nexusfuture.currency.dto.ai.RagAnswerDto;
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

    /**
     * 【基础版】RAG 问答
     * 流程：接收问题 -> 调用 Python 检索 -> 拼接基础 Prompt -> 请求 LLM -> 返回纯文本答案
     *
     * @param question 用户提问
     * @return AI 生成的回答字符串
     */
    public String answerQuestion(String question) {
        log.info("📝 [RAG] 收到用户问题: {}", question);

        // 1. 向量检索：从 Python 服务获取最相关的文档片段
        List<PythonSearchClient.SearchResult> chunks = pythonSearchClient.search(question, 5);

        if (chunks.isEmpty()) {
            log.warn("⚠️ [RAG] 未检索到任何相关文档片段！");
            return "抱歉，没有在知识库中找到相关的文档信息来回答您的问题。";
        }

        log.info("📚 [RAG] 成功检索到 {} 个相关片段", chunks.size());

        // 2. 上下文组装：将多个片段合并为一个字符串，并标注来源
        String context = chunks.stream()
                .map(chunk -> String.format("[来源: %s, 页码: %d] %s", 
                    chunk.getDocumentName(), 
                    chunk.getPage() != null ? chunk.getPage() : 0,
                    chunk.getContent()))
                .collect(Collectors.joining("\n\n"));

        // 3. Prompt 填充：使用基础模板拼接指令、上下文和问题
        String prompt = String.format(PromptConstants.BASIC_RAG, context, question);

        log.info("🤖 [RAG] 准备将 Prompt 发送给通义千问 LLM...");

        // 4. LLM 生成：获取最终答案
        String answer = tongyiQwenClient.getChatCompletion(prompt);
        
        log.info("✨ [RAG] LLM 返回最终答案: {}", answer);
        return answer;
    }

    /**
     * 【增强版】带引用溯源的 RAG 问答
     * 流程：接收问题 -> 调用 Python 检索 -> 拼接企业级 Prompt -> 请求 LLM -> 封装答案与证据链
     *
     * @param question 用户提问
     * @return 包含答案和引用列表的结构化对象
     */
    public RagAnswerDto answerWithCitations(String question) {
        log.info("📝 [RAG-Plus] 收到增强型问答请求: {}", question);

        // 1. 向量检索
        List<PythonSearchClient.SearchResult> chunks = pythonSearchClient.search(question, 5);

        if (chunks.isEmpty()) {
            RagAnswerDto dto = new RagAnswerDto();
            dto.setAnswer("抱歉，没有在知识库中找到相关的文档信息。");
            return dto;
        }

        // 2. 上下文组装：为每个片段添加标准化的元数据标签
        String context = chunks.stream()
                .map(chunk -> String.format("[来源: %s, 页码: %d] %s", 
                    chunk.getDocumentName(), 
                    chunk.getPage() != null ? chunk.getPage() : 0,
                    chunk.getContent()))
                .collect(Collectors.joining("\n\n"));

        // 3. Prompt 填充：使用更严谨的企业级模板
        String prompt = String.format(PromptConstants.ENTERPRISE_RAG, context, question);

        // 4. LLM 生成
        String answer = tongyiQwenClient.getChatCompletion(prompt);

        // 5. 结果封装：构建包含“答案+证据”的 DTO
        RagAnswerDto resultDto = new RagAnswerDto();
        resultDto.setAnswer(answer);
        
        // 转换引用格式，供前端展示溯源信息
        List<RagAnswerDto.Citation> citations = chunks.stream().map(chunk -> {
            RagAnswerDto.Citation citation = new RagAnswerDto.Citation();
            citation.setDocumentName(chunk.getDocumentName());
            citation.setPage(chunk.getPage());
            citation.setContent(chunk.getContent());
            citation.setSimilarity(chunk.getSimilarity());
            return citation;
        }).collect(Collectors.toList());
        
        resultDto.setCitations(citations);

        log.info("✨ [RAG-Plus] 完成回答，包含 {} 个引用来源", citations.size());
        return resultDto;
    }
}
