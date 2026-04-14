package com.nexusfuture.currency.client;

import com.nexusfuture.currency.dto.ai.TongyiRequest;
import com.nexusfuture.currency.dto.ai.TongyiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class TongyiQwenClient {

    private final RestTemplate restTemplate;

    // 从 application.properties 中注入配置
    @Value("${ai.tongyi.api-key}")
    private String apiKey;

    @Value("${ai.tongyi.url}")
    private String apiUrl;

    public TongyiQwenClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 调用通义千问大模型获取聊天回复。
     *
     * @param prompt 用户输入的问题。
     * @return AI生成的文本回复。
     */
    public String getChatCompletion(String prompt) {
        log.info("正在为 prompt 调用通义千问 API: '{}'", prompt);

        // 1. 设置HTTP请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // API Key 需要放在 Authorization 头中，格式为 "Bearer YOUR_API_KEY"
        headers.set("Authorization", "Bearer " + apiKey);

        // 2. 构建请求体
        TongyiRequest requestPayload = TongyiRequest.createWithPrompt(prompt);

        // 3. 将请求头和请求体封装成 HttpEntity
        HttpEntity<TongyiRequest> requestEntity = new HttpEntity<>(requestPayload, headers);

        try {
            // 4. 发送POST请求并获取响应
            TongyiResponse response = restTemplate.postForObject(apiUrl, requestEntity, TongyiResponse.class);

            // 5. 从响应中提取生成的文本
            if (response != null && response.getGeneratedText() != null) {
                log.info("成功从通义千问获取到回复。");
                return response.getGeneratedText();
            } else {
                log.warn("通义千问返回了空的响应或文本。");
                return "抱歉，AI未能生成有效的回复。";
            }
        } catch (Exception e) {
            log.error("调用通义千问API时发生错误: {}", e.getMessage(), e);
            // 在生产环境中，应该抛出更具体的自定义异常
            throw new RuntimeException("调用AI服务失败，请检查配置或网络连接。", e);
        }
    }
}