package com.nexusfuture.currency.controller;

import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.dto.BocRateDto;
import com.nexusfuture.currency.dto.CurrencyRateDto;
import com.nexusfuture.currency.service.BocExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/boc-rate")
@RequiredArgsConstructor
public class BocRateController {

    private final BocExchangeRateService bocExchangeRateService;

    @GetMapping("/latest")
    public Result<List<CurrencyRateDto>> getLatestRates() {
        log.info("📥 [API] 请求最新中行汇率数据");
        
        return bocExchangeRateService.getLatestRatesFromCacheOrDb()
                .map(rateList -> {
                    log.info("📤 [API] 成功返回中行汇率数据，共 {} 条记录", rateList.size());
                    return Result.success(rateList);
                })
                .orElse(Result.fail(404, "暂无汇率数据，请稍后再试"));
    }

    @GetMapping("/history")
    public Result<List<BocRateDto>> getHistoryRates(
            @RequestParam String currencyCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        log.info("📥 [API] 请求历史汇率 - 货币: {}, 日期范围: {} ~ {}", 
                currencyCode, startDate, endDate);
        
        try {
            List<BocRateDto> history = bocExchangeRateService.getHistoryRates(
                    currencyCode, startDate, endDate);
            
            log.info("📤 [API] 返回 {} 条历史汇率记录", history.size());
            return Result.success(history);
            
        } catch (Exception e) {
            log.error("❌ [API] 查询历史汇率失败", e);
            return Result.fail(500, "查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public Result<String> refreshRates() {
        log.info("🔄 [API] 手动触发中行汇率刷新");
        
        try {
            bocExchangeRateService.fetchAndPersistLatestRates();
            log.info("✅ [API] 汇率刷新成功");
            return Result.success("汇率数据已成功刷新");
            
        } catch (Exception e) {
            log.error("❌ [API] 汇率刷新失败", e);
            return Result.fail(500, "刷新失败: " + e.getMessage());
        }
    }
}
