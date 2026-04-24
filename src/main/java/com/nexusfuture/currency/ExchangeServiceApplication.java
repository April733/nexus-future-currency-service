package com.nexusfuture.currency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; // 导入注解

@SpringBootApplication
@EnableJpaAuditing // 开启 JPA 审计功能，自动处理创建时间和修改时间
@EnableAspectJAutoProxy // 加上这一行
public class ExchangeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExchangeServiceApplication.class, args);
    }
}