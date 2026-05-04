package com.nexusfuture.currency.scheduler;

import com.nexusfuture.currency.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 汇率定时任务触发器。
 * <p>
 * 这是一个纯粹的调度器，其唯一职责是在指定时间触发核心业务逻辑。
 * 它不包含任何实际的业务实现，而是委托给 {@link ExchangeRateService} 来执行。
 * 这种分离使得业务逻辑可以被其他组件（如 API 控制器）轻松复用。
 */
@Component
public class ExchangeRateScheduler {

    // 手写 log，彻底告别 Lombok 问题！
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateScheduler.class);

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateScheduler(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * 每个工作日（周一至周五）的 17:30 CET 执行。
     * <p>
     * 触发 {@link ExchangeRateService#fetchAndPersistLatestRate()} 方法。
     *
     * @see <a href="https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/index.en.html">ECB Exchange Rates</a>
     */
    @Scheduled(cron = "0 0 9 ? * MON-FRI")
    public void fetch() {
        log.info("开始执行定时任务：触发汇率拉取与持久化...");
        try {
            exchangeRateService.fetchAndPersistLatestRate();
            log.info("定时任务成功完成：汇率业务逻辑已执行。");
        } catch (Exception e) {
            log.error("汇率拉取定时任务执行失败。", e);
        }
    }
}