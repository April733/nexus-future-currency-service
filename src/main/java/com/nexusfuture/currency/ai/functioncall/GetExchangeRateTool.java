package com.nexusfuture.currency.ai.functioncall;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper; // 🔥 1. 确保导入
import com.nexusfuture.currency.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor // 🔥 2. 它会自动为下面的 final 字段生成构造函数
public class GetExchangeRateTool implements ExecutableTool {

    private final ExchangeRateService exchangeRateService;
    private final ObjectMapper objectMapper; // 🔥 3. 增加这一行，Spring 会自动注入

    @Override
    public String getName() {
        return "get_exchange_rate";
    }

    @Override
    public String getDescription() {
        return "获取实时外汇汇率";
    }

    @Override
    public Map<String, Object> getParameters() {
        // 定义工具所需的参数结构 (JSON Schema 格式)
        Map<String, Object> properties = new java.util.HashMap<>();
        
        Map<String, Object> fromCurrency = new java.util.HashMap<>();
        fromCurrency.put("type", "string");
        fromCurrency.put("description", "源货币代码，例如 USD");
        properties.put("fromCurrency", fromCurrency);

        Map<String, Object> toCurrency = new java.util.HashMap<>();
        toCurrency.put("type", "string");
        toCurrency.put("description", "目标货币代码，例如 CNY");
        properties.put("toCurrency", toCurrency);

        Map<String, Object> parameters = new java.util.HashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", java.util.List.of("fromCurrency", "toCurrency"));
        
        return parameters;
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
                // 现在 objectMapper 可以正常使用了
                return objectMapper.writeValueAsString(Map.of("error", "执行工具时发生内部错误: " + e.getMessage()));
            } catch (JsonProcessingException jsonProcessingException) {
                return "{\"error\": \"执行工具时发生内部错误，并且序列化错误信息也失败了。\"}";
            }
        }
    }
}
