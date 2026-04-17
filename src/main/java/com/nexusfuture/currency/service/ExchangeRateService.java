package com.nexusfuture.currency.service;

import com.nexusfuture.currency.client.EcbExchangeRateHttpClient;
import com.nexusfuture.currency.config.RedisProperties;
import com.nexusfuture.currency.constant.RedisKeyConstant;
import com.nexusfuture.currency.entity.ExchangeRate;
import com.nexusfuture.currency.repository.ExchangeRateRepository;
import com.nexusfuture.currency.util.CurrencyXmlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 汇率业务的核心服务。
 * <p>
 * 封装了从外部数据源获取汇率、持久化到数据库以及更新缓存的完整业务流程。
 * 这个服务是汇率相关操作的唯一入口点，可被定时任务、API控制器等多个组件复用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository repository;
    private final EcbExchangeRateHttpClient ecbHttpClient;
    private final ObjectProvider<RedisCacheService> cacheProvider;
    private final ObjectProvider<RedisProperties> redisPropertiesProvider;

    /**
     * 执行一次完整的汇率获取、持久化和缓存更新流程。
     * 这是该服务的主要业务方法。
     */
    public void fetchAndPersistLatestRate() {
        log.info("开始执行业务逻辑：拉取并持久化欧洲央行每日汇率...");
        try {
            // 1. 从外部客户端获取原始 XML 数据
            String xml = ecbHttpClient.fetchDailyXml();
            String url = ecbHttpClient.getSourceUrl();

            // 2. 构建实体并持久化到数据库
            ExchangeRate rate = new ExchangeRate();
            rate.setUrl(url);
            rate.setRawXml(xml);
            // 时间直接存字符串
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            rate.setCreateTime(now);
            repository.save(rate);
            log.info("汇率数据成功保存到数据库：{}", rate);

            // 3. 如果 Redis 缓存服务和相关配置都可用，则更新缓存
            cacheProvider.ifAvailable(cache -> {
                redisPropertiesProvider.ifAvailable(redisProperties -> {

                    // 3.1. 先删除所有旧的汇率缓存
                    cache.deleteByPrefix(RedisKeyConstant.CURRENCY_ECB);
                    log.info("旧缓存 {} 已删除", RedisKeyConstant.CURRENCY_ECB);

                    // 3.2. 写入缓存
                    cache.set(RedisKeyConstant.CURRENCY_ECB, CurrencyXmlParser.parseToCurrencyRateList(xml), RedisKeyConstant.TTL);

                    log.info("Redis 已写入当日缓存 {}，XML={}，TTL={}", RedisKeyConstant.CURRENCY_ECB, xml, RedisKeyConstant.TTL);

                });
            });
        } catch (Exception e) {
            log.error("拉取并持久化每日汇率失败：{}", e.getMessage(), e);
            // 向上抛出运行时异常，以便调用方（如 Scheduler）可以捕获并记录更高层次的失败
            throw new RuntimeException("Failed to fetch and persist latest exchange rate.", e);
        }
    }


    /**
     * 获取最新汇率 XML
     * 缓存优先 → 数据库兜底 → 自动回填缓存
     */
    public Optional<String> findLatestRate() {
        RedisCacheService cache = cacheProvider.getIfAvailable();
        RedisProperties redisProperties = redisPropertiesProvider.getIfAvailable();

        // 1. 先读缓存
        if (cache != null && redisProperties != null) {
            Optional<String> xmlOpt = cache.get(RedisKeyConstant.CURRENCY_ECB, String.class);
            if (xmlOpt.isPresent()) {
                log.info("缓存命中，返回 XML {}", xmlOpt.get());
                log.info("xmlOpt，XML={}", xmlOpt);
                return xmlOpt;
            }
        }

        // 2. 缓存未命中 → 查库
        log.warn("缓存未命中，查询数据库");
        Optional<ExchangeRate> dbOpt = repository.findFirstByOrderByCreateTimeDesc();
        if (dbOpt.isEmpty()) {
            return Optional.empty();
        }

        ExchangeRate rate = dbOpt.get();
        String raw = CurrencyXmlParser.parseToCurrencyRateList(rate.getRawXml());

        // 3. 回填缓存
        if (cache != null && redisProperties != null) {
            try {
                cache.set(RedisKeyConstant.CURRENCY_ECB, raw, RedisKeyConstant.TTL);
                log.info("已自动回填 Redis 缓存，XML={}", raw);

            } catch (Exception e) {
                log.error("回填缓存失败", e);
            }
        }
        return Optional.of(raw);
    }

}