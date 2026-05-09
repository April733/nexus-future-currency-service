package com.nexusfuture.currency.service;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfuture.currency.client.BocCrawlerClient;
import com.nexusfuture.currency.constant.RedisKeyConstant;
import com.nexusfuture.currency.dto.BocRateDto;
import com.nexusfuture.currency.entity.BocExchangeRate;
import com.nexusfuture.currency.mapper.BocExchangeRateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BocExchangeRateService {

    private final BocCrawlerClient bocCrawlerClient;
    private final BocExchangeRateMapper bocExchangeRateMapper;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<RedisCacheService> cacheProvider;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void fetchAndPersistLatestRates() {
        log.info(" [BOC Service] 开始执行业务逻辑：拉取并持久化中行汇率...");

        try {
            log.info(" [BOC Service] 步骤 1：开始调用爬虫获取数据...");
            List<BocExchangeRate> rates = bocCrawlerClient.crawlAndParse();
            log.info(" [BOC Service] 步骤 2：爬虫返回数据量: {}", rates != null ? rates.size() : 0);

            if (rates == null || rates.isEmpty()) {
                log.warn("️ [BOC Service] 未获取到任何汇率数据，跳过持久化");
                return;
            }

            String rateDate = rates.get(0).getRateDate();
            log.info("️ [BOC Service] 步骤 3：正在删除日期为 {} 的旧数据...", rateDate);
            
            int deletedCount = bocExchangeRateMapper.deleteByRateDate(rateDate);
            log.info("✅ [BOC Service] 步骤 4：已删除 {} 条旧记录", deletedCount);

            log.info(" [BOC Service] 步骤 5：开始执行 JDBC 批量插入（分批模式）...");
            batchInsertWithJdbc(rates);
            log.info(" [BOC Service] 步骤 6：批量插入完成");

            invalidateAndRefreshCache(rates);
            log.info(" [BOC Service] 步骤 7：缓存更新完成");

        } catch (Exception e) {
            log.error(" [BOC Service] 拉取并持久化中行汇率失败", e);
            throw new RuntimeException("Failed to fetch and persist BOC exchange rates.", e);
        }
    }

    /**
     * 🔥 使用 JDBC Batch + 分批处理 进行高效批量插入
     * 建议：每 50 条为一个批次，平衡性能与稳定性
     */
    private void batchInsertWithJdbc(List<BocExchangeRate> rates) {
        String sql = "INSERT INTO boc_exchange_rate (currency_code, currency_name, spot_buy, cash_buy, spot_sell, cash_sell, conversion_rate, rate_date, publish_time, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        //  使用 Hutool 将大列表拆分为每 50 条一个小列表
        List<List<BocExchangeRate>> batches = CollUtil.split(rates, 50);
        
        log.info(" [BOC Service] 数据已拆分为 {} 个批次，每批最多 50 条，开始执行...", batches.size());

        long totalStartTime = System.currentTimeMillis();
        int totalInserted = 0;
        int batchIndex = 1;

        for (List<BocExchangeRate> batch : batches) {
            long batchStartTime = System.currentTimeMillis();
            log.info(" [BOC Service] >>> 正在处理第 {}/{} 批，数量: {}", batchIndex, batches.size(), batch.size());

            jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    BocExchangeRate rate = batch.get(i);
                    ps.setString(1, rate.getCurrencyCode());
                    ps.setString(2, rate.getCurrencyName());
                    ps.setBigDecimal(3, rate.getSpotBuy());
                    ps.setBigDecimal(4, rate.getCashBuy());
                    ps.setBigDecimal(5, rate.getSpotSell());
                    ps.setBigDecimal(6, rate.getCashSell());
                    ps.setBigDecimal(7, rate.getConversionRate());
                    ps.setString(8, rate.getRateDate());
                    ps.setString(9, rate.getPublishTime());
                    ps.setString(10, rate.getCreateTime());
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });

            long batchEndTime = System.currentTimeMillis();
            long batchDuration = batchEndTime - batchStartTime;
            totalInserted += batch.size();
            
            log.info(" [BOC Service] <<< 第 {} 批执行完成，耗时: {} ms", batchIndex, batchDuration);
            batchIndex++;
        }

        long totalEndTime = System.currentTimeMillis();
        log.info(" [BOC Service] 所有批次执行完毕！总计插入 {} 条数据，总耗时: {} ms", totalInserted, (totalEndTime - totalStartTime));
    }

    private void invalidateAndRefreshCache(List<BocExchangeRate> rates) {
        cacheProvider.ifAvailable(cache -> {
            try {
                cache.delete(RedisKeyConstant.CURRENCY_BOC);
                log.info("🗑️ [BOC Service] 已清除旧缓存");
                
                List<BocRateDto> dtoList = rates.stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList());
                
                String json = objectMapper.writeValueAsString(dtoList);
                cache.set(RedisKeyConstant.CURRENCY_BOC, json, Duration.ofSeconds(3600));
                
                log.info("💾 [BOC Service] 新缓存已写入，TTL=3600s");
            } catch (Exception e) {
                log.error("❌ [BOC Service] 缓存更新失败", e);
            }
        });
    }

    private BocRateDto convertToDto(BocExchangeRate entity) {
        return new BocRateDto(
                entity.getCurrencyCode(),
                entity.getCurrencyName(),
                entity.getSpotBuy(),
                entity.getCashBuy(),
                entity.getSpotSell(),
                entity.getCashSell(),
                entity.getConversionRate(),
                entity.getRateDate(),
                entity.getPublishTime()
        );
    }

    public Optional<String> getLatestRatesFromCacheOrDb() {
        RedisCacheService cache = cacheProvider.getIfAvailable();

        if (cache != null) {
            Optional<String> cached = cache.get(RedisKeyConstant.CURRENCY_BOC, String.class);
            if (cached.isPresent()) {
                log.info("🎯 [BOC Service] 缓存命中，返回中行汇率数据");
                return cached;
            }
        }

        log.warn("⚠️ [BOC Service] 缓存未命中，查询数据库");
        
        List<BocExchangeRate> latestRates = bocExchangeRateMapper.findTopNByOrderByCreateTimeDesc(20);

        if (latestRates.isEmpty()) {
            return Optional.empty();
        }

        try {
            List<BocRateDto> dtoList = latestRates.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            String json = objectMapper.writeValueAsString(dtoList);

            if (cache != null) {
                cache.set(RedisKeyConstant.CURRENCY_BOC, json, Duration.ofSeconds(3600));
                log.info("💾 [BOC Service] 已自动回填 Redis 缓存");
            }

            return Optional.of(json);

        } catch (Exception e) {
            log.error("序列化汇率数据失败", e);
            return Optional.empty();
        }
    }

    public List<BocRateDto> getHistoryRates(String currencyCode, String startDate, String endDate) {
        List<BocExchangeRate> entities = bocExchangeRateMapper.findByCurrencyCodeAndRateDateBetween(
                currencyCode, startDate, endDate);

        return entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}
