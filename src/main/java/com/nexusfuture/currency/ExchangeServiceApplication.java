package com.nexusfuture.currency;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.nexusfuture.currency.config.BocCrawlerProperties;
import com.nexusfuture.currency.config.EcbHttpProperties;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication()
@MapperScan("com.nexusfuture.currency.mapper")
@EnableAspectJAutoProxy
@EnableScheduling
@EnableConfigurationProperties({EcbHttpProperties.class, BocCrawlerProperties.class})
public class ExchangeServiceApplication {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    public static void main(String[] args) {
        SpringApplication.run(ExchangeServiceApplication.class, args);
    }

    /**
     * 应用启动后检查 Redis 连接配置
     */
    @Bean
    public ApplicationRunner checkRedisConnection() {
        return args -> {
            log.info("========================================");
            log.info("Redis 连接配置检查");
            log.info("Redis Host: {}", redisHost);
            log.info("Redis Port: {}", redisPort);
            log.info("Redis Address: {}:{}", redisHost, redisPort);
            
            // 安全检查：禁止连接本地 Redis
            if ("localhost".equalsIgnoreCase(redisHost) || 
                "127.0.0.1".equals(redisHost) || 
                "0.0.0.0".equals(redisHost)) {
                log.error("========================================");
                log.error("⚠️  安全警告：检测到尝试连接本地 Redis！");
                log.error("⚠️  当前配置: {}:{} ", redisHost, redisPort);
                log.error("⚠️  生产环境禁止连接本地 Redis，请修改 spring.data.redis.host 配置");
                log.error("========================================");
                throw new IllegalStateException(
                    "Security Error: Cannot connect to local Redis. " +
                    "Current config: " + redisHost + ":" + redisPort + ". " +
                    "Please configure remote Redis server."
                );
            }
            
            log.info("✅ Redis 连接地址验证通过");
            log.info("========================================");
        };
    }

    /**
     * MyBatis-Plus 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}