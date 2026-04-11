package com.nexusfuture.currency.config;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * {@link WebClient} 基于 Reactor Netty，适合非阻塞与组合异步；在 MVC 项目中常单独引入 webflux starter 只为了使用 WebClient。
 */
@Configuration
@RequiredArgsConstructor
public class EcbWebClientConfig {

    private final EcbHttpProperties properties;

    @Bean
    public WebClient ecbWebClient() {
        HttpClient reactorHttpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.getConnectTimeout().toMillis())
                .responseTimeout(properties.getReadTimeout());

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(reactorHttpClient))
                .build();
    }
}
