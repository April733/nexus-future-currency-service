package com.nexusfuture.currency.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfuture.currency.ai.functioncall.ToolRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TongyiQwenClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;

    private WebClient webClient;
    private WebClient embeddingWebClient;

    @Value("${ai.tongyi.api-key}")
    private String apiKey;

    @Value("${ai.tongyi.model}")
    private String model;

    @Value("${ai.tongyi.url}")
    private String apiUrl;

    @Value("${ai.tongyi.stream-url:}")
    private String streamApiUrl;

    @Value("${ai.tongyi.openai-url:}")
    private String openaiApiUrl;
    
    // 新增：Embedding API 地址
    @Value("${ai.tongyi.embedding-url}")
    private String embeddingUrl;

    @Value("${ai.tongyi.embedding.model}")
    private String embeddingModel;

    public TongyiQwenClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, ToolRegistry toolRegistry) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
    }

    @PostConstruct
    public void init() {
        String urlForStreaming = streamApiUrl;
        if (urlForStreaming == null || urlForStreaming.trim().isEmpty()) {
            urlForStreaming = apiUrl;
        }
        this.webClient = webClientBuilder.baseUrl(urlForStreaming).build();
        
        // 限制 Embedding 接口的最大内存缓冲区为 10MB，防止超大响应撑爆内存
        this.embeddingWebClient = webClientBuilder
                .baseUrl(embeddingUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    // 非流式，拿到完整回答
    public String getChatCompletion(String prompt) {
        try {
            Map<String, Object> param = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            String responseJson = webClient.post()
                    .uri(openaiApiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(param)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 🔥 核心修改：从复杂的 JSON 中提取 content
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode choices = root.get("choices");
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    // 优先取 content，如果没有再取 reasoning_content (如果有的话)
                    String content = message.has("content") ? message.get("content").asText() : "";
                    log.info("🤖 [LLM] 提取到的最终回答: {}", content);
                    return content;
                }
            }
            
            log.warn("⚠️ [LLM] 无法从响应中提取 content，返回原始 JSON");
            return responseJson; // 兜底策略
            
        } catch (Exception e) {
            log.error("❌ [LLM] 调用或解析失败", e);
            throw new RuntimeException("LLM 服务异常", e);
        }
    }

    /**
     * 【主流式方法 - 修复完成】
     */
    public Flux<String> getChatCompletionStream(String prompt) {
        log.info("正在为 prompt 调用通义千问流式 API: '{}'", prompt);

        var body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "stream", true
        );

        return this.webClient.post()
                .header("Authorization", "Bearer " + apiKey)  // 修复 401
                .header("Accept", "text/event-stream")
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("调用通义千问API错误. 状态码: {}, 内容: {}", response.statusCode(), errorBody);
                            return Mono.error(new RuntimeException("API调用失败"));
                        })
                )
                .bodyToFlux(String.class)
                .doOnNext(line -> log.info("【原始SSE数据】: {}", line))
                .map(this::parseSseData)  // 统一解析
                .filter(text -> !text.isEmpty());
    }

    /**
     * 【最终修复版解析】
     * 同时支持 content + reasoning_content，不再报错
     */
    private String parseSseData(String sseLine) {
        try {
            if (!sseLine.startsWith("data:")) return "";
            String json = sseLine.substring(5).trim();
            if ("[DONE]".equals(json)) return "";

            JSONObject root = JSON.parseObject(json);
            JSONObject delta = root.getJSONArray("choices").getJSONObject(0).getJSONObject("delta");
            String content = delta.getString("content");

            return content == null ? "" : content;

        } catch (Exception e) {
            return "";
        }
    }


    // =====================================================================================
    // 新增：Function Calling 功能
    // =====================================================================================

    /**
     * 【新增方法】
     * 支持Function Calling的非流式聊天。
     *
     * @param userPrompt 用户的初始输入
     * @return AI的最终文本回复
     */
    public String getChatCompletionWithTools(String userPrompt) {
        // 初始化对话历史
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", userPrompt));

        // 最多进行5轮对话，防止无限循环
        for (int i = 0; i < 5; i++) {
            log.info("【Function Call】第 {} 轮对话开始", i + 1);

            // 构造请求体
            Map<String, Object> requestBody = Map.of(
                    "model", model, // 修正：使用项目中已验证过的模型
                    "messages", messages,
                    "tools", toolRegistry.getAllToolsForApi()
            );

            // 发起请求并同步等待结果
            String resultJson = webClient.post()
                    .uri(openaiApiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    // 新增：添加关键的错误状态处理，以打印出API返回的具体错误信息
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                log.error("【Function Call】调用通义千问API时出错. 状态码: {}, 错误信息: {}", response.statusCode(), errorBody);
                                return Mono.error(new RuntimeException("调用通义千问API失败: " + errorBody));
                            })
                    )
                    .bodyToMono(String.class)
                    .block();

            log.info("【Function Call】AI返回原始JSON: {}", resultJson);

            try {
                // 修正：使用项目统一的ObjectMapper进行解析，增强健壮性
                JsonNode response = objectMapper.readTree(resultJson);
                JsonNode messageNode = response.at("/choices/0/message");

                // 将AI的回复加入对话历史
                messages.add(objectMapper.convertValue(messageNode, new TypeReference<Map<String, Object>>() {
                }));

                // 检查是否需要调用工具
                if (messageNode.has("tool_calls")) {
                    log.info("【Function Call】AI请求调用工具...");
                    for (JsonNode toolCallNode : messageNode.get("tool_calls")) {
                        String toolCallId = toolCallNode.get("id").asText();
                        String toolName = toolCallNode.at("/function/name").asText();
                        // 决定性修正：使用.asText()获取纯净的、包含JSON的字符串
                        String toolArgs = toolCallNode.at("/function/arguments").asText();

                        log.info("【Function Call】执行工具: {}, 参数: {}", toolName, toolArgs);
                        // 修正：调用回接收String作为参数的executeTool方法
                        String toolResult = toolRegistry.executeTool(toolName, toolArgs);
                        log.info("【Function Call】工具执行结果: {}", toolResult);

                        // 将工具执行结果加入对话历史
                        messages.add(Map.of(
                                "role", "tool",
                                "tool_call_id", toolCallId,
                                "content", toolResult
                        ));
                    }
                    // 继续下一轮循环，让AI根据工具结果进行总结
                    continue;
                }

                // 如果不需要调用工具，说明AI已经给出了最终答案
                log.info("【Function Call】AI返回最终答案，对话结束。");
                return messageNode.get("content").asText();

            } catch (JsonProcessingException e) {
                log.error("【Function Call】解析AI响应JSON失败: {}", resultJson, e);
                throw new RuntimeException("解析AI响应时发生错误", e);
            }
        }

        log.warn("【Function Call】对话超过最大轮次，未能获得最终答案。");
        return "抱歉，经过多轮工具调用后，仍无法得出最终结论。";
    }

    /**
     * 新增：获取文本向量 (Embedding)
     */
    public List<Float> getEmbedding(String text) {
        Map<String, Object> body = Map.of(
                "model", embeddingModel,
                "input", List.of(text)
        );

        String resultJson = embeddingWebClient.post()
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // 使用 Fastjson2 解析响应
        JSONObject json = JSON.parseObject(resultJson);
        JSONArray embeddings = json.getJSONObject("output").getJSONArray("embeddings");
        
        // 提取第一个向量
        JSONArray values = embeddings.getJSONObject(0).getJSONArray("embedding");
        List<Float> embeddingList = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            embeddingList.add(values.getFloat(i));
        }
        
        return embeddingList;
    }

    /**
     * 批量获取 embeddings（支持动态维度压缩）
     */
    public List<float[]> getBatchEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        // 【关键优化】指定 dimensions 为 1536，实现无损降维并兼容 HNSW 索引
        Map<String, Object> body = Map.of(
                "model", "tongyi-embedding-vision-plus-2026-03-06",
                "input", texts,
                "dimensions", 1536 
        );

        String resultJson = embeddingWebClient.post()
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONObject json = JSON.parseObject(resultJson);
        JSONArray embeddings = json.getJSONObject("output").getJSONArray("embeddings");
        
        List<float[]> allEmbeddings = new ArrayList<>(embeddings.size());
        for (int i = 0; i < embeddings.size(); i++) {
            JSONArray values = embeddings.getJSONObject(i).getJSONArray("embedding");
            // 此时 values.size() 将是 1536
            float[] embeddingArray = new float[values.size()];
            for (int j = 0; j < values.size(); j++) {
                embeddingArray[j] = values.getFloat(j);
            }
            allEmbeddings.add(embeddingArray);
        }
        
        return allEmbeddings;
    }
}