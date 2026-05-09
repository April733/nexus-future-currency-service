package com.nexusfuture.currency.scheduler;

import com.nexusfuture.currency.service.BocExchangeRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 中国银行外汇牌价定时任务触发器。
 * <p>
 * 这是一个纯粹的调度器，其唯一职责是在指定时间触发核心业务逻辑。
 * 它不包含任何实际的业务实现，而是委托给 {@link BocExchangeRateService} 来执行。
 * 这种分离使得业务逻辑可以被其他组件（如 API 控制器）轻松复用。
 */
@Component
public class BocRateScheduler {

    private static final Logger log = LoggerFactory.getLogger(BocRateScheduler.class);

    private final BocExchangeRateService bocExchangeRateService;

    public BocRateScheduler(BocExchangeRateService bocExchangeRateService) {
        this.bocExchangeRateService = bocExchangeRateService;
    }

    /**
     * 每小时执行一次（整点触发）。
     * <p>
     * Cron 表达式说明：0 0 * * * ?
     * - 秒：0
     * - 分：0
     * - 时：*（每小时）
     * - 日：*（每天）
     * - 月：*（每月）
     * - 周：?（不指定）
     * <p>
     * 触发 {@link BocExchangeRateService#fetchAndPersistLatestRates()} 方法。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void fetchHourly() {
        log.info("⏰ [BOC Scheduler] 开始执行定时任务：触发中行汇率拉取与持久化...");
        try {
            bocExchangeRateService.fetchAndPersistLatestRates();
            log.info("✅ [BOC Scheduler] 定时任务成功完成：中行汇率业务逻辑已执行。");
        } catch (Exception e) {
            log.error("❌ [BOC Scheduler] 中行汇率拉取定时任务执行失败。", e);
        }
    }

    /**
     * 【可选】工作日的早上 9:30 额外执行一次（银行开盘时间）。
     * <p>
     * Cron 表达式说明：0 30 9 ? * MON-FRI
     * - 在周一至周五的 9:30:00 执行
     */
    // @Scheduled(cron = "0 30 9 ? * MON-FRI")
    public void fetchAtMarketOpen() {
        log.info("⏰ [BOC Scheduler] 开始执行开盘时段汇率拉取...");
        try {
            bocExchangeRateService.fetchAndPersistLatestRates();
            log.info("✅ [BOC Scheduler] 开盘时段汇率拉取成功。");
        } catch (Exception e) {
            log.error("❌ [BOC Scheduler] 开盘时段汇率拉取失败。", e);
        }
    }
}
