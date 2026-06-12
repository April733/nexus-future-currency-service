package com.nexusfuture.currency.ai.functioncall;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfuture.currency.dto.CurrencyRateDto;
import com.nexusfuture.currency.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetExchangeRateTool implements ExecutableTool {

    private final ExchangeRateService exchangeRateService;
    private final ObjectMapper objectMapper;

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
            Optional<List<CurrencyRateDto>> latestRateOpt = exchangeRateService.findLatestRate();
            if (latestRateOpt.isPresent()) {
                List<CurrencyRateDto> rateList = latestRateOpt.get();
                log.info("Successfully executed getExchangeRate tool, found {} rates", rateList.size());
                
                String fromCurrency = (String) arguments.get("fromCurrency");
                String toCurrency = (String) arguments.get("toCurrency");
                
                CurrencyRateDto targetRate = rateList.stream()
                    .filter(rate -> rate.getCode().equalsIgnoreCase(toCurrency))
                    .findFirst()
                    .orElse(null);
                
                if (targetRate != null) {
                    return objectMapper.writeValueAsString(Map.of(
                        "code", targetRate.getCode(),
                        "chineseName", targetRate.getChineseName(),
                        "englishName", targetRate.getEnglishName(),
                        "rate", targetRate.getRate()
                    ));
                } else {
                    return objectMapper.writeValueAsString(Map.of(
                        "error", "未找到货币 " + toCurrency + " 的汇率数据",
                        "availableCurrencies", rateList.stream().map(CurrencyRateDto::getCode).toList()
                    ));
                }
            } else {
                log.warn("No exchange rate data found.");
                return objectMapper.writeValueAsString(Map.of("error", "未找到任何汇率数据。"));
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
