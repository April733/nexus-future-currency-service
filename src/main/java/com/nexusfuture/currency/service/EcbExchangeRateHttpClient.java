package com.nexusfuture.currency.service;

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
     * 供调度器等调用：按 {@link EcbHttpProperties#getClient()} 选择实现。
     */
    public String fetchDailyXml() {
        return switch (properties.getClient()) {
            case REST_TEMPLATE -> fetchDailyXmlWithRestTemplate();
            case WEB_CLIENT_ASYNC -> fetchDailyXmlWithWebClientAsync().join();
        };
    }

    public String getSourceUrl() {
        return properties.getUrl();
    }

    /**
     * 同步路径：当前线程一直阻塞到 ECB 返回或超时/重试耗尽。
     */
    public String fetchDailyXmlWithRestTemplate() {
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
                    sleepQuietly();
                }
            }
        }
        throw new CompletionException("ECB RestTemplate 在 " + properties.getMaxAttempts() + " 次尝试后仍失败", last);
    }

    /**
     * 异步路径：立即返回 Future；真正 I/O 在 Reactor 线程上执行，可用 thenApply / thenAccept 链式组合。
     * 若在 {@link org.springframework.scheduling.annotation.Scheduled} 中需要结果，需 {@code .join()} 或 {@code get()}。
     */
    public CompletableFuture<String> fetchDailyXmlWithWebClientAsync() {
        return fetchMono(1).toFuture();
    }

    /**
     * 失败时按 {@link EcbHttpProperties#getRetryBackoff()} 延迟再试，最多 {@link EcbHttpProperties#getMaxAttempts()} 次。
     */
    private Mono<String> fetchMono(int attempt) {
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
                            .then(Mono.defer(() -> fetchMono(attempt + 1)));
                });
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(properties.getRetryBackoff().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
    }
}
