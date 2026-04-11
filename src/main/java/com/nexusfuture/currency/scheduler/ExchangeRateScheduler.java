package com.nexusfuture.currency.scheduler;

import com.nexusfuture.currency.entity.ExchangeRate;
import com.nexusfuture.currency.repository.ExchangeRateRepository;
import com.nexusfuture.currency.service.EcbExchangeRateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private static final DateTimeFormatter FETCH_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExchangeRateRepository repository;
    private final EcbExchangeRateHttpClient ecbHttpClient;

    @Scheduled(cron = "0 0 5 * * ?")
    public void fetch() {
        try {
            String xml = ecbHttpClient.fetchDailyXml();
            String url = ecbHttpClient.getSourceUrl();

            ExchangeRate rate = new ExchangeRate();
            LocalDateTime now = LocalDateTime.now();
            rate.setDate(now.format(FETCH_TIME_FORMAT));
            rate.setUrl(url);
            rate.setRawXml(xml);
            rate.setCreateTime(now);

            repository.save(rate);
            log.info("汇率保存成功：{}", rate.getDate());
        } catch (Exception e) {
            log.error("拉取失败：{}", e.getMessage(), e);
        }
    }
}
