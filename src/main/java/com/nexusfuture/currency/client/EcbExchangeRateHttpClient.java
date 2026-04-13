package com.nexusfuture.currency.client;

import com.nexusfuture.currency.config.EcbHttpProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * ECB XML 拉取：对比 {@link RestTemplate}（同步）与 {@link WebClient} + {@link CompletableFuture}（异步包装），
 * 并带简单重试与超时（超时在 RestTemplate/WebClient 层配置，重试在本类用循环 / Reactor 组合实现）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcbExchangeRateHttpClient {

    private final EcbHttpProperties properties;
    private final RestTemplate restTemplate;
    private final WebClient ecbWebClient;

    /**
     * 供定时器等调用：按 {@link EcbHttpProperties#getClient()} 选择实现。
     */
    public String fetchDailyXml() {
        return switch (properties.getClient()) {
            case REST_TEMPLATE -> fetchDailyXmlSync();
            case WEB_CLIENT_ASYNC -> fetchDailyXmlAsync().join();
        };
    }

    public String getSourceUrl() {
        return properties.getUrl();
    }

    /**
     * 同步获取 XML 数据。
     * <p>
     * 使用 {@link RestTemplate}，当前线程会阻塞直到请求完成或失败。
     * 内部包含重试逻辑。
     *
     * @return 从 ECB 获取的 XML 字符串
     */
    private String fetchDailyXmlSync() {
        Exception last = null;
        for (int attempt = 1; attempt <= properties.getMaxAttempts(); attempt++) {
            try {
                String xml = restTemplate.getForObject(properties.getUrl(), String.class);
                if (xml == null || xml.isBlank()) {
                    throw new IllegalStateException("Empty response body");
                }
                if (attempt > 1) {
                    log.info("ECB RestTemplate 在第 {} 次尝试成功", attempt);
                }
                return xml;
            } catch (Exception e) {
                last = e;
                log.warn("ECB RestTemplate 第 {} 次失败: {}", attempt, e.getMessage());
                if (attempt < properties.getMaxAttempts()) {
                    waitForRetry();
                }
            }
        }
        throw new CompletionException("ECB RestTemplate 在 " + properties.getMaxAttempts() + " 次尝试后仍失败", last);
    }

    /**
     * 异步获取 XML 数据。
     * <p>
     * 使用 {@link WebClient}，立即返回一个 {@link CompletableFuture}，I/O 操作在独立的线程池上执行。
     *
     * @return 一个包含 XML 字符串的 CompletableFuture
     */
    private CompletableFuture<String> fetchDailyXmlAsync() {
        return fetchWithRetryAsync(1).toFuture();
    }

    /**
     * 使用 WebClient 执行异步获取，并包含完整的重试逻辑。
     * <p>
     * 这是一个递归的响应式方法。当请求失败时，它会延迟一段时间后再次调用自身，直到达到最大尝试次数。
     *
     * @param attempt 当前的尝试次数
     * @return 一个包含结果或最终错误的 {@link Mono}
     */
    private Mono<String> fetchWithRetryAsync(int attempt) {
        return ecbWebClient.get()
                .uri(properties.getUrl())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(properties.getReadTimeout())
                .doOnError(ex -> log.warn("ECB WebClient 第 {} 次失败: {}", attempt, ex.toString()))
                .onErrorResume(ex -> {
                    if (attempt >= properties.getMaxAttempts()) {
                        return Mono.error(new CompletionException(
                                "ECB WebClient 在 " + properties.getMaxAttempts() + " 次尝试后仍失败", ex));
                    }
                    return Mono.delay(properties.getRetryBackoff())
                            .then(Mono.defer(() -> fetchWithRetryAsync(attempt + 1)));
                });
    }

    /**
     * 在同步重试模式下，使当前线程暂停一段时间。
     */
    private void waitForRetry() {
        try {
            Thread.sleep(properties.getRetryBackoff().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("Retry wait was interrupted", e);
        }
    }
}