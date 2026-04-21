package com.nexusfuture.currency.dto.ai;

import lombok.Data;
import java.util.List;

/**
 * RAG 增强型问答响应对象
 * 用于承载 AI 生成的答案及其对应的原始引用来源，支持前端实现“溯源”功能。
 */
@Data
public class RagAnswerDto {
    
    /** AI 生成的最终总结性回答 */
    private String answer;
    
    /** 引用证据列表，按相关性排序 */
    private List<Citation> citations;

    /**
     * 内部类：引用来源详情
     */
    @Data
    public static class Citation {
        /** 来源文档名称 (e.g., forex_regulations.pdf) */
        private String documentName;
        
        /** 来源页码 */
        private Integer page;
        
        /** 原始片段内容 (用于前端高亮展示) */
        private String content;
        
        /** 向量相似度分数 (0-1之间，越高越相关) */
        private Double similarity;
    }
}
