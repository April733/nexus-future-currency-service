package com.nexusfuture.currency.scheduler;

import com.nexusfuture.currency.entity.ExchangeRate;
import com.nexusfuture.currency.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    // ✅ URL 提取出来
    public static final String ECB_RATE_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

    private final ExchangeRateRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    // 每天 5:00 执行
    @Scheduled(cron = "0 0 5 * * ?")
    public void fetch() {
        try {
            // 1. 拉取原始XML
            String xml = restTemplate.getForObject(ECB_RATE_URL, String.class);

            // 2. 保存数据（包含 URL）
            ExchangeRate rate = new ExchangeRate();
            rate.setDate(LocalDate.now().toString());
            rate.setUrl(ECB_RATE_URL);  // ✅ 保存URL
            rate.setRawXml(xml);
            rate.setCreateTime(LocalDateTime.now());

            repository.save(rate);
            System.out.println("✅ 汇率保存成功：" + LocalDate.now());

        } catch (Exception e) {
            System.err.println("❌ 拉取失败：" + e.getMessage());
        }
    }
}