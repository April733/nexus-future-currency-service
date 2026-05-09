package com.nexusfuture.currency.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 中国银行外汇牌价爬虫配置
 */
@ConfigurationProperties(prefix = "currency.boc")
@Getter
@Setter
public class BocCrawlerProperties {

    /** 中国银行外汇牌价页面 URL */
    private String url = "https://www.boc.cn/sourcedb/whpj/index.html";

    /** HTTP 连接超时 */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /** HTTP 读取超时 */
    private Duration readTimeout = Duration.ofSeconds(30);

    /** 最大重试次数 */
    private int maxAttempts = 3;

    /** 重试间隔 */
    private Duration retryBackoff = Duration.ofMillis(1000);

    /** User-Agent（模拟浏览器） */
    private String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
}
