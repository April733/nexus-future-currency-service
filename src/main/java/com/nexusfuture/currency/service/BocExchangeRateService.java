package com.nexusfuture.currency.service;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfuture.currency.client.BocCrawlerClient;
import com.nexusfuture.currency.common.CurrencyInfoEnum;
import com.nexusfuture.currency.constant.RedisKeyConstant;
import com.nexusfuture.currency.dto.BocRateDto;
import com.nexusfuture.currency.dto.CurrencyRateDto;
import com.nexusfuture.currency.entity.BocExchangeRate;
import com.nexusfuture.currency.mapper.BocExchangeRateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * 获取最新中行汇率数据（返回 CurrencyRateDto 列表，与 ECB 保持一致）
     */
    public Optional<List<CurrencyRateDto>> getLatestRatesFromCacheOrDb() {
        RedisCacheService cache = cacheProvider.getIfAvailable();

        if (cache != null) {
            try {
                Optional<String> cachedJsonOpt = cache.get(RedisKeyConstant.CURRENCY_BOC, String.class);
                if (cachedJsonOpt.isPresent()) {
                    List<CurrencyRateDto> cachedList = parseBocJsonToCurrencyRateList(cachedJsonOpt.get());
                    
                    // 🔥 检查是否是旧格式数据（rate > 100 说明是未转换的 BOC 原始数据）
                    boolean isOldData = cachedList.stream()
                        .anyMatch(dto -> dto.getRate() != null && dto.getRate().compareTo(new BigDecimal("100")) > 0);
                    
                    if (isOldData) {
                        log.warn("⚠️ [BOC Service] 检测到旧格式缓存，正在删除...");
                        cache.delete(RedisKeyConstant.CURRENCY_BOC);
                    } 
                    // 🔥 检查缓存数量是否不足（应该至少有12条：13种货币 - EUR基准）
                    else if (cachedList.size() < 12) {
                        log.warn("️ [BOC Service] 检测到缓存数量不足（{} 条 < 12 条），正在删除...", cachedList.size());
                        cache.delete(RedisKeyConstant.CURRENCY_BOC);
                    } 
                    else if (!cachedList.isEmpty()) {
                        log.info("🎯 [BOC Service] 缓存命中，返回 {} 条中行汇率记录", cachedList.size());
                        return Optional.of(cachedList);
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [BOC Service] 读取缓存失败，将查询数据库", e);
            }
        }

        log.warn("⚠️ [BOC Service] 缓存未命中，查询数据库");
        
        try {
            // 根据枚举中定义的货币代码查询数据库
            List<CurrencyRateDto> currencyRateList = queryAndConvertByEnum();

            if (cache != null && !currencyRateList.isEmpty()) {
                cache.set(RedisKeyConstant.CURRENCY_BOC, currencyRateList, Duration.ofSeconds(3600));
                log.info("💾 [BOC Service] 已自动回填 Redis 缓存，共 {} 条记录", currencyRateList.size());
            }

            return Optional.of(currencyRateList);

        } catch (Exception e) {
            log.error("❌ [BOC Service] 查询并转换汇率数据失败", e);
            return Optional.empty();
        }
    }

    /**
     * 根据枚举中定义的货币代码查询数据库并转换为 CurrencyRateDto
     */
    private List<CurrencyRateDto> queryAndConvertByEnum() {
        //  获取所有枚举定义的货币（排除CNY），同时包含英文代码和中文名称
        List<String> targetCodes = java.util.Arrays.stream(CurrencyInfoEnum.values())
            .filter(e -> !"CNY".equals(e.name()))  // 排除CNY
            .flatMap(e -> java.util.stream.Stream.of(
                e.name(),                    // 英文代码：AUD, GBP, HKD...
                e.getChineseName()          // 中文名称：澳大利亚元, 英镑, 港币...
            ))
            .collect(Collectors.toList());
        
        log.info("[BOC Service] 开始查询枚举定义的 {} 种货币（含中英文，不含CNY）", targetCodes.size());
        
        // 🔥 批量查询：使用 INNER JOIN + 子查询确保每种货币只返回最新一条
        List<BocExchangeRate> rates = bocExchangeRateMapper.findLatestByCurrencyCodes(targetCodes);
        
        log.info("[BOC Service] 从数据库批量查询到 {} 条记录", rates.size());
        
        if (rates.isEmpty()) {
            log.warn("[BOC Service] 未查询到任何汇率数据");
            return new ArrayList<>();
        }
        
        // 🔥 按 currency_code 去重（优先使用英文代码的记录）
        Map<String, BocExchangeRate> uniqueRates = new LinkedHashMap<>();
        for (BocExchangeRate rate : rates) {
            String code = rate.getCurrencyCode();
            
            log.info("[BOC Service] 处理记录: currencyCode={}, currencyName={}", 
                rate.getCurrencyCode(), rate.getCurrencyName());
            
            // 如果 currency_code 为 NULL，尝试从 currency_name 转换
            if (code == null || code.isEmpty()) {
                String name = rate.getCurrencyName();
                CurrencyInfoEnum info = CurrencyInfoEnum.fromName(name);
                if (info != null) {
                    code = info.name();
                    log.info("[BOC Service] 从中文名称 '{}' 转换为英文代码 '{}'", name, code);
                } else {
                    log.warn("[BOC Service] 无法识别的货币名称: {}", name);
                    continue;
                }
            }
            
            // 如果已经有这条货币的记录，跳过
            if (!uniqueRates.containsKey(code)) {
                uniqueRates.put(code, rate);
                log.info("[BOC Service] 添加到结果集: {}", code);
            } else {
                log.info("[BOC Service] 跳过重复货币: {}", code);
            }
        }
        
        log.info("[BOC Service] 去重后剩余 {} 种货币: {}", uniqueRates.size(), uniqueRates.keySet());
        
        // 获取 EUR 汇率
        BigDecimal eurCnyRate = uniqueRates.entrySet().stream()
            .filter(entry -> "EUR".equalsIgnoreCase(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .map(r -> r.getSpotBuy())  // 🔥 使用 spot_buy
            .orElse(null);
        
        if (eurCnyRate == null || eurCnyRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("[BOC Service] ❌ 未找到有效的 EUR 汇率，无法进行转换");
            return new ArrayList<>();
        }
        
        log.info("[BOC Service] ✅ 使用 EUR/CNY 汇率: {} 进行转换", eurCnyRate);
        
        final int SCALE = 6;
        final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
        
        List<CurrencyRateDto> result = new ArrayList<>();
        
        for (Map.Entry<String, BocExchangeRate> entry : uniqueRates.entrySet()) {
            String currencyCode = entry.getKey();
            BocExchangeRate rate = entry.getValue();
            
            // 🔥 跳过 EUR 本身
            if ("EUR".equalsIgnoreCase(currencyCode)) {
                log.info("[BOC Service] 跳过 EUR（基准货币）");
                continue;
            }
            
            // 🔥 尝试从英文代码或中文名称获取枚举
            CurrencyInfoEnum info = CurrencyInfoEnum.fromCode(currencyCode);
            if (info == null) {
                // 如果英文代码匹配失败，尝试从中文名称匹配
                info = CurrencyInfoEnum.fromName(currencyCode);
            }
            
            if (info == null) {
                log.warn("[BOC Service] 跳过未在枚举中的货币: {}", currencyCode);
                continue;
            }
            
            BigDecimal bocRateValue = rate.getSpotBuy();  // 🔥 使用 spot_buy
            
            if (bocRateValue == null || bocRateValue.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("[BOC Service] 跳过无效汇率: {} = {}", currencyCode, bocRateValue);
                continue;
            }
            
            try {
                //  转换逻辑（严格按照数学推导）：
                // BOC: 100 XXX = Y CNY → curr_spot = Y
                // BOC: 100 EUR = Z CNY → eur_spot = Z
                // 
                // 最终公式：targetRate = eur_spot / curr_spot
                // 含义：1 EUR = targetRate XXX
                
                BigDecimal ecbRate = eurCnyRate.divide(bocRateValue, SCALE, ROUNDING_MODE);
                
                result.add(new CurrencyRateDto(
                    info.name(),  // 🔥 使用枚举的英文名称作为 code
                    info.getEnglishName(),
                    info.getChineseName(),
                    ecbRate
                ));
                
                log.info("[BOC Service] 成功转换: {} = {}", info.name(), ecbRate);
                
            } catch (ArithmeticException e) {
                log.error("[BOC Service] 转换汇率失败: currency={}, rate={}", currencyCode, bocRateValue, e);
            }
        }
        
        // 🔥 手动添加 CNY（相对于 EUR 的汇率）
        if (eurCnyRate != null && eurCnyRate.compareTo(BigDecimal.ZERO) > 0) {
            // 1 EUR = eurCnyRate/100 CNY
            BigDecimal cnyRate = eurCnyRate.divide(new BigDecimal("100"), SCALE, ROUNDING_MODE);
            CurrencyInfoEnum cnyInfo = CurrencyInfoEnum.CNY;
            result.add(new CurrencyRateDto(
                "CNY",
                cnyInfo.getEnglishName(),
                cnyInfo.getChineseName(),
                cnyRate
            ));
            log.info("[BOC Service] 手动添加 CNY 汇率: {}", cnyRate);
        }
        
        log.info("[BOC Service] 成功转换 {} 条汇率为 ECB 基准", result.size());
        return result;
    }

    /**
     * 将查询到的汇率转换为 ECB 基准（对欧元的汇率）
     */
    private List<CurrencyRateDto> convertToEcbBase(List<CurrencyRateDto> rawRates) {
        if (rawRates.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询 EUR 汇率
        BocExchangeRate eurRate = bocExchangeRateMapper.findLatestByCurrencyCode("EUR");
        
        if (eurRate == null) {
            log.error("[BOC Service] ❌ 数据库中未找到 EUR 汇率，无法进行转换");
            return new ArrayList<>();
        }
        
        BigDecimal eurCnyRate = eurRate.getConversionRate() != null ? 
            eurRate.getConversionRate() : eurRate.getSpotBuy();
        
        if (eurCnyRate == null || eurCnyRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("[BOC Service] ❌ EUR 汇率无效: {}", eurCnyRate);
            return new ArrayList<>();
        }
        
        log.info("[BOC Service] ✅ 使用 EUR/CNY 汇率: {} 进行转换", eurCnyRate);
        
        final BigDecimal HUNDRED = new BigDecimal("100");
        final int SCALE = 6;
        final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
        
        List<CurrencyRateDto> convertedRates = new ArrayList<>();
        
        for (CurrencyRateDto dto : rawRates) {
            try {
                BigDecimal bocRateValue = dto.getRate();
                
                // 转换逻辑：ecbRate = eurCnyRate * 100 / bocRate
                BigDecimal cnyPerUnit = bocRateValue.divide(HUNDRED, SCALE + 2, ROUNDING_MODE);
                BigDecimal ecbRate = eurCnyRate.divide(cnyPerUnit, SCALE, ROUNDING_MODE);
                
                convertedRates.add(new CurrencyRateDto(
                    dto.getCode(),
                    dto.getEnglishName(),
                    dto.getChineseName(),
                    ecbRate
                ));
                
            } catch (ArithmeticException e) {
                log.error("[BOC Service] 转换汇率失败: currency={}, rate={}", dto.getCode(), dto.getRate(), e);
            }
        }
        
        log.info("[BOC Service] 成功转换 {} 条汇率为 ECB 基准", convertedRates.size());
        return convertedRates;
    }

    /**
     * 从 BOC 数据中获取 EUR 的 CNY 汇率
     */
    private BigDecimal getEurCnyRate(List<BocExchangeRate> bocRates) {
        return bocRates.stream()
            .filter(rate -> {
                String code = rate.getCurrencyCode();
                String name = rate.getCurrencyName();
                
                // 支持多种匹配方式：EUR、欧元、Euro
                boolean codeMatch = "EUR".equalsIgnoreCase(code) || 
                                   "欧元".equals(code);
                boolean nameMatch = "欧元".equals(name) || 
                                   "Euro".equalsIgnoreCase(name);
                
                return codeMatch || nameMatch;
            })
            .findFirst()
            .map(rate -> {
                BigDecimal rateValue = rate.getConversionRate() != null ? 
                    rate.getConversionRate() : rate.getSpotBuy();
                
                if (rateValue == null || rateValue.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("[BOC Service] EUR 汇率值无效: {}", rateValue);
                    return null;
                }
                
                log.info("[BOC Service] 找到 EUR 汇率: currencyCode={}, currencyName={}, rate={}", 
                    rate.getCurrencyCode(), rate.getCurrencyName(), rateValue);
                return rateValue;
            })
            .orElse(null);
    }

    /**
     * 将中行汇率实体列表转换为 CurrencyRateDto 列表
     * 并将汇率转换为与 ECB 相同的基准（对欧元的汇率）
     */
    private List<CurrencyRateDto> convertBocToCurrencyRateList(List<BocExchangeRate> bocRates) {
        List<CurrencyRateDto> result = new ArrayList<>();
        
        // 获取 EUR 的汇率作为转换基准
        BigDecimal eurCnyRate = getEurCnyRate(bocRates);
        
        if (eurCnyRate == null || eurCnyRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("[BOC Service] ❌ 未找到有效的 EUR/CNY 汇率，无法进行转换。请检查数据库中是否有欧元数据！");
            return result;
        }
        
        log.info("[BOC Service] ✅ 使用 EUR/CNY 汇率: {} 进行转换", eurCnyRate);
        
        // 预计算常量
        final BigDecimal HUNDRED = new BigDecimal("100");
        final int SCALE = 6; // 保留6位小数，与ECB对齐
        final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
        
        int skipCount = 0;
        int convertCount = 0;
        
        for (BocExchangeRate bocRate : bocRates) {
            String currencyCode = bocRate.getCurrencyCode();
            String currencyName = bocRate.getCurrencyName();
            
            // 尝试通过代码或名称匹配枚举
            CurrencyInfoEnum info = CurrencyInfoEnum.fromCode(currencyCode);
            if (info == null) {
                // 如果代码匹配失败，尝试通过名称匹配
                info = CurrencyInfoEnum.fromName(currencyCode);
            }
            if (info == null) {
                // 再尝试通过 currency_name 匹配
                info = CurrencyInfoEnum.fromName(currencyName);
            }
            
            if (info == null) {
                log.debug("跳过未在枚举中定义的货币: code={}, name={}", currencyCode, currencyName);
                skipCount++;
                continue;
            }
            
            String standardCode = info.name(); // 获取标准货币代码（如 USD、EUR）
            
            // 跳过 EUR 本身
            if ("EUR".equalsIgnoreCase(standardCode)) {
                continue;
            }
            
            // 使用中间价作为汇率，如果没有则使用现汇买入价
            BigDecimal bocRateValue = bocRate.getConversionRate() != null ? 
                bocRate.getConversionRate() : bocRate.getSpotBuy();
            
            if (bocRateValue == null || bocRateValue.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("跳过无效汇率: {} = {}", standardCode, bocRateValue);
                skipCount++;
                continue;
            }
            
            try {
                // 转换逻辑：
                // BOC: 100 XXX = Y CNY → 1 XXX = Y/100 CNY
                // ECB: 1 EUR = Z XXX
                // 已知: 1 EUR = eurCnyRate CNY
                // 所以: 1 EUR = (eurCnyRate / (Y/100)) XXX = (eurCnyRate * 100 / Y) XXX
                
                // 步骤1: 计算单位外币对人民币的汇率 (1 XXX = ? CNY)
                BigDecimal cnyPerUnit = bocRateValue.divide(HUNDRED, SCALE + 2, ROUNDING_MODE);
                
                // 步骤2: 计算对欧元的汇率 (1 EUR = ? XXX)
                BigDecimal ecbRate = eurCnyRate.divide(cnyPerUnit, SCALE, ROUNDING_MODE);
                
                result.add(new CurrencyRateDto(
                    standardCode,
                    info.getEnglishName(),
                    info.getChineseName(),
                    ecbRate
                ));
                
                convertCount++;
                
            } catch (ArithmeticException e) {
                log.error("[BOC Service] 汇率转换计算异常: currency={}, bocRate={}", standardCode, bocRateValue, e);
                skipCount++;
            }
        }
        
        log.info("[BOC Service] 转换完成: 成功 {} 条, 跳过 {} 条", convertCount, skipCount);
        return result;
    }

    /**
     * 将 JSON 字符串解析为 CurrencyRateDto 列表
     */
    private List<CurrencyRateDto> parseBocJsonToCurrencyRateList(String json) {
        try {
            return objectMapper.readValue(json, 
                new com.fasterxml.jackson.core.type.TypeReference<List<CurrencyRateDto>>() {});
        } catch (Exception e) {
            log.error("❌ [BOC Service] 解析 JSON 字符串为 DTO 列表失败", e);
            return new ArrayList<>();
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
