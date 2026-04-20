package com.nexusfuture.currency.ai.functioncall;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfuture.currency.service.ExchangeRateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class GetExchangeRateTool implements ExecutableTool {

    private final ExchangeRateService exchangeRateService;
    private final ObjectMapper objectMapper;

    public GetExchangeRateTool(ExchangeRateService exchangeRateService, ObjectMapper objectMapper) {
        this.exchangeRateService = exchangeRateService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "getExchangeRate";
    }

    @Override
    public String getDescription() {
        return "获取最新的所有货币相对于欧元的汇率。";
    }

    @Override
    public Map<String, Object> getParameters() {
        return Map.of("type", "object", "properties", Collections.emptyMap());
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        log.info("Executing getExchangeRate tool...");
        try {
            Optional<String> latestRateOpt = exchangeRateService.findLatestRate();
            if (latestRateOpt.isPresent()) {
                log.info("Successfully executed getExchangeRate tool, result: {}", latestRateOpt.get());
                return latestRateOpt.get();
            } else {
                log.warn("No exchange rate data found.");
                return "{\"error\": \"未找到任何汇率数据。\"}";
            }
        } catch (Exception e) {
            log.error("Error executing getExchangeRate tool", e);
            try {
                return objectMapper.writeValueAsString(Map.of("error", "执行工具时发生内部错误: " + e.getMessage()));
            } catch (JsonProcessingException jsonProcessingException) {
                return "{\"error\": \"执行工具时发生内部错误，并且序列化错误信息也失败了。\"}";
            }
        }
    }
}
