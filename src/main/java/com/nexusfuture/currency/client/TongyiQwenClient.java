package com.nexusfuture.currency.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfuture.currency.dto.ai.TongyiRequest;
import com.nexusfuture.currency.dto.ai.TongyiResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TongyiQwenClient {

    private final RestTemplate restTemplate;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private WebClient webClient;

    @Value("${ai.tongyi.api-key}")
    private String apiKey;

    @Value("${ai.tongyi.url}")
    private String apiUrl;

    @Value("${ai.tongyi.stream-url:}")
    private String streamApiUrl;

    public TongyiQwenClient(RestTemplate restTemplate, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        String urlForStreaming = streamApiUrl;
        if (urlForStreaming == null || urlForStreaming.trim().isEmpty()) {
            log.warn("Configuration 'ai.tongyi.stream-url' not found. Falling back to 'ai.tongyi.url' for streaming. This may not work as expected if endpoints differ.");
            urlForStreaming = apiUrl;
        }
        log.info("Initializing WebClient for streaming with base URL: {}", urlForStreaming);
        this.webClient = webClientBuilder.baseUrl(urlForStreaming).build();
    }

    //    /**
//     * 【非流式方法】
//     */
//    public String getChatCompletion(String prompt) {
//        log.info("正在为 prompt 调用通义千问 API: '{}'", prompt);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("Authorization", "Bearer " + apiKey);
//
//        TongyiRequest requestPayload = TongyiRequest.createWithPrompt(prompt);
//        HttpEntity<TongyiRequest> requestEntity = new HttpEntity<>(requestPayload, headers);
//
//        try {
//            TongyiResponse response = restTemplate.postForObject(apiUrl, requestEntity, TongyiResponse.class);
//
//            if (response != null && response.getOutput() != null && response.getOutput().getText() != null) {
//                log.info("成功从通义千问获取到回复。");
//                return response.getOutput().getText();
//            } else {
//                log.warn("通义千问返回了空的响应或文本。");
//                return "抱歉，AI未能生成有效的回复。";
//            }
//        } catch (Exception e) {
//            log.error("调用通义千问API时发生错误: {}", e.getMessage(), e);
//            throw new RuntimeException("调用AI服务失败，请检查配置或网络连接。", e);
//        }
//    }
// 非流式，拿到完整回答
    public String getChatCompletion(String prompt) {
        Map<String, Object> param = Map.of(
                "model", "qwen3.5-flash",
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        String result = webClient.post()
                .uri("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(param)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        System.out.println("完整AI结果：" + result);
        return result;
    }

    /**
     * 【主流式方法 - 修复完成】
     */
    public Flux<String> getChatCompletionStream(String prompt) {
        log.info("正在为 prompt 调用通义千问流式 API: '{}'", prompt);

        var body = Map.of(
                "model", "qwen3.5-flash",
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
}