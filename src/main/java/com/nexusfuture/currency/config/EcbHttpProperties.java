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
    private String url = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

    /** TCP 连接超时 */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /** 等待响应体（含读）超时；RestTemplate 为 read timeout，WebClient 为 responseTimeout */
    private Duration readTimeout = Duration.ofSeconds(30);

    /** 总尝试次数（含第一次） */
    private int maxAttempts = 3;

    /** 失败后再试前的间隔 */
    private Duration retryBackoff = Duration.ofMillis(500);

    /**
     * REST_TEMPLATE：同步阻塞，Servlet 栈常用。<br>
     * WEB_CLIENT_ASYNC：Reactor Netty 上发请求，对外返回 CompletableFuture（定时任务里仍会 join 等待完成）。
     */
    private ClientMode client = ClientMode.REST_TEMPLATE;

    public enum ClientMode {
        REST_TEMPLATE,
        WEB_CLIENT_ASYNC
    }
}
