package com.nexusfuture.currency.controller;

import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.entity.ExchangeRate;
import com.nexusfuture.currency.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}