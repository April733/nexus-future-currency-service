package com.nexusfuture.currency.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * {@link RestTemplate} 基于 JDK {@link java.net.HttpURLConnection}，同步阻塞；通过 factory 设置连接/读超时。
 */
@Configuration
@EnableConfigurationProperties(EcbHttpProperties.class)
@RequiredArgsConstructor
public class RestTemplateClientConfig {

    private final EcbHttpProperties properties;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        factory.setReadTimeout((int) properties.getReadTimeout().toMillis());
        return new RestTemplate(factory);
    }
}
