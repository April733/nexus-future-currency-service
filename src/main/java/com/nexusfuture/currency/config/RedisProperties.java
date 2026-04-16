package com.nexusfuture.currency.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "currency.redis")
@Getter
@Setter
public class RedisProperties {

    /** 为 true 时注册 Lettuce 连接并写入缓存；false 时不连 Redis（默认，便于本地测试）。 */
    private boolean enabled = false;

    private String host = "127.0.0.1";
    private int port = 6379;
    private String password = "";
    private int database = 0;

}
