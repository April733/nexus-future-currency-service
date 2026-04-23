package com.nexusfuture.currency.controller;

import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.dto.HistoryRateDto;
import com.nexusfuture.currency.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rate")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    /**
     * 获取最新的汇率数据。
     * <p>
     * 该实现利用了 Optional 的链式调用，代码更简洁、安全。
     *
     * @return 包含最新汇率数据的 Result 对象，如果找不到则返回失败的 Result。
     */
    @GetMapping("/getLatestRate")
    public Result<?> getLatestRate() {
        return exchangeRateService.findLatestRate()
                .map(xml -> {
                    log.info("成功获取最新汇率数据，来源：{}", xml);
                    return Result.success(xml);
                })
                .orElse(Result.fail(404, "无法获取到最新的汇率数据"));
    }

    /**
     * 手动触发一次汇率数据的拉取和持久化。
     * <p>
     * 该接口会调用与定时任务完全相同的业务方法，用于手动更新数据。
     *
     * @return 表示操作结果的 Result 对象。
     */
    @GetMapping("/refill")
    public Result<String> refill() {
        log.info("手动触发汇率数据拉取任务...");
        try {
            exchangeRateService.fetchAndPersistLatestRate();
            log.info("手动触发汇率数据拉取任务成功。");
            return Result.success("汇率数据拉取和缓存填充任务已成功触发。");
        } catch (Exception e) {
            log.error("手动触发汇率数据拉取任务失败。", e);
            return Result.fail(500, "任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发拉取指定日期范围的汇率数据。
     * <p>
     * 该接口使用 ECB SDMX API，支持通过参数指定开始和结束日期。
     *
     * @param currency 货币代码 (e.g., "USD", "CNY")
     * @param startDate 开始日期 (格式: YYYY-MM-DD)
     * @param endDate 结束日期 (格式: YYYY-MM-DD)
     * @return 表示操作结果的 Result 对象。
     */
    @PostMapping("/fetch-range")
    public Result<String> fetchRateByRange(@RequestParam String currency, 
                                           @RequestParam String startDate, 
                                           @RequestParam String endDate) {
        log.info("收到手动拉取请求: 货币={}, {} -> {}", currency, startDate, endDate);
        try {
            exchangeRateService.fetchAndPersistRateByRange(currency, startDate, endDate);
            return Result.success(String.format("已成功拉取 %s (%s 到 %s) 的汇率数据", currency, startDate, endDate));
        } catch (Exception e) {
            log.error("拉取指定范围汇率失败", e);
            return Result.fail(500, "拉取失败: " + e.getMessage());
        }
    }

    /**
     * 增量开发：查询历史汇率走势
     */
    @GetMapping("/history")
    public Result<List<HistoryRateDto>> getHistory(
            @RequestParam String currency,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            List<HistoryRateDto> history = exchangeRateService.getHistoryRates(currency, startDate, endDate);
            return Result.success(history); 
        } catch (Exception e) {
            log.error("查询历史汇率失败", e);
            return Result.fail(500, "查询失败: " + e.getMessage());
        }
    }
}