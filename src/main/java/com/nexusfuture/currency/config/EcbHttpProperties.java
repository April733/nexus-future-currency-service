package com.nexusfuture.currency.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * ECB 日频 XML 的 HTTP 行为：客户端类型、超时与简单重试。
 */
@ConfigurationProperties(prefix = "currency.ecb")
@Getter
@Setter
public class EcbHttpProperties {

    /** ECB eurofxref-daily.xml */
    private String url;

    /** TCP 连接超时 */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /** 等待响应体（含读）超时；RestTemplate 为 read timeout，WebClient 为 responseTimeout */
    private Duration readTimeout = Duration.ofSeconds(30);

    /** 总尝试次数（含第一次） */
    private int maxAttempts = 3;

    /** 失败后再试前的间隔 */
    private Duration retryBackoff = Duration.ofMillis(500);

    /**
     * 客户端类型：rest_template 或 web_client_async
     */
    private ClientType client = ClientType.REST_TEMPLATE;

    /** ECB SDMX API 基础地址模板 (用于按日期范围查询，%s 为货币代码占位符) */
    private String smdxUrlTemplate;

    public enum ClientType {
        REST_TEMPLATE,
        WEB_CLIENT_ASYNC
    }
}
