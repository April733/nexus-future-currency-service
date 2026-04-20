package com.nexusfuture.currency.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class PythonSearchClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // 🔥 直接从配置文件读取 Python 服务地址
    public PythonSearchClient(
            @Value("${embedding.service.url:http://localhost:8000}") String baseUrl,
            ObjectMapper objectMapper) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public List<SearchResult> search(String query, int topK) {
        try {
            log.info("🚀 [Java -> Python] 正在请求检索: \"{}\", TopK: {}", query, topK);

            SearchRequest request = new SearchRequest();
            request.setQuery(query);
            request.setTopK(topK);

            String responseJson = webClient.post()
                    .uri("/search")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("✅ [Python -> Java] 收到检索响应");
            return parseResults(responseJson);
        } catch (Exception e) {
            log.error("❌ [Java] 调用 Python 搜索服务失败: {}", e.getMessage(), e);
            throw new RuntimeException("RAG 检索服务不可用", e);
        }
    }

    private List<SearchResult> parseResults(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode resultsNode = root.get("results");
            
            if (resultsNode == null || !resultsNode.isArray()) {
                return Collections.emptyList();
            }

            List<SearchResult> results = new ArrayList<>();
            for (JsonNode node : resultsNode) {
                SearchResult result = new SearchResult();
                result.setDocumentName(node.has("document_name") ? node.get("document_name").asText() : "Unknown");
                result.setChunkIndex(node.has("chunk_index") ? node.get("chunk_index").asInt() : 0);
                result.setContent(node.has("content") ? node.get("content").asText() : "");
                
                // 🔥 兼容 similarity 或 score 字段
                if (node.has("similarity")) {
                    result.setSimilarity(node.get("similarity").asDouble());
                } else if (node.has("score")) {
                    result.setSimilarity(node.get("score").asDouble());
                }

                result.setPage(node.has("page") ? node.get("page").asInt() : null);
                results.add(result);
            }
            return results;
        } catch (Exception e) {
            log.error("解析 Python 响应失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Data
    public static class SearchRequest {
        private String query;
        private int topK;
    }

    @Data
    public static class SearchResult {
        private String documentName;
        private Integer chunkIndex;
        private String content;
        private Double similarity;
        private Integer page;
    }
}
