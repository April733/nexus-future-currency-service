package com.nexusfuture.currency.service;

import com.nexusfuture.currency.client.EcbExchangeRateHttpClient;
import com.nexusfuture.currency.config.RedisProperties;
import com.nexusfuture.currency.constant.RedisKeyConstant;
import com.nexusfuture.currency.dto.HistoryRateDto;
import com.nexusfuture.currency.entity.ExchangeRate;
import com.nexusfuture.currency.repository.ExchangeRateRepository;
import com.nexusfuture.currency.util.CurrencyXmlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository repository; // 仅保留用于每日快照（如果还需要的话）
    private final EcbExchangeRateHttpClient ecbHttpClient;
    private final ObjectProvider<RedisCacheService> cacheProvider;
    private final ObjectProvider<RedisProperties> redisPropertiesProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * RowMapper: 将 ResultSet 映射为 HistoryRateDto
     */
    private final RowMapper<HistoryRateDto> historyRowMapper = (rs, rowNum) -> {
        // 使用构造函数直接初始化，避免依赖 Setter
        return new HistoryRateDto(
            rs.getString("rate_date"),
            rs.getBigDecimal("rate_value")
        );
    };

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

    /**
     * 拉取并持久化指定日期范围的汇率 (全 JDBC 实现)
     */
    public void fetchAndPersistRateByRange(String currency, String startDate, String endDate) {
        log.info("开始执行业务逻辑：拉取 {} 的汇率 ({} 至 {})...", currency, startDate, endDate);
        try {
            String xmlData = ecbHttpClient.fetchXmlByDateRange(currency, startDate, endDate);
            parseAndSaveHistoryJdbc(xmlData, currency, startDate, endDate);
        } catch (Exception e) {
            log.error("拉取指定范围汇率失败", e);
            throw new RuntimeException("Failed to fetch rate by range.", e);
        }
    }

    /**
     * 解析 XML 并通过 JDBC 批量入库
     */
    private void parseAndSaveHistoryJdbc(String xmlData, String currency, String startDate, String endDate) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        Document doc = builder.parse(new ByteArrayInputStream(xmlData.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        String namespace = "http://www.sdmx.org/resources/sdmxml/schemas/v2_1/data/generic";
        NodeList obsNodes = doc.getElementsByTagNameNS(namespace, "Obs");

        // 统一时间格式
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<Object[]> batchArgs = new ArrayList<>();
        for (int i = 0; i < obsNodes.getLength(); i++) {
            Node obsNode = obsNodes.item(i);
            if (obsNode.getNodeType() != Node.ELEMENT_NODE) continue;
            
            Element obsElement = (Element) obsNode;
            String date = getAttributeValue(obsElement, namespace, "ObsDimension", "value");
            String rate = getAttributeValue(obsElement, namespace, "ObsValue", "value");

            if (date != null && rate != null) {
                batchArgs.add(new Object[]{currency, date, new BigDecimal(rate), nowStr});
            }
        }

        if (!batchArgs.isEmpty()) {
            log.info("解析完成，共 {} 条记录。准备执行 JDBC 原子操作...", batchArgs.size());

            // 1. JDBC 原子删除
            String deleteSql = "DELETE FROM exchange_rate_history WHERE currency_code = ? AND rate_date BETWEEN ? AND ?";
            int deletedCount = jdbcTemplate.update(deleteSql, currency, startDate, endDate);
            log.info("已清理货币 {} 在 {} 到 {} 期间的 {} 条旧记录", currency, startDate, endDate, deletedCount);

            // 2. JDBC 批量插入
            String insertSql = "INSERT INTO exchange_rate_history (currency_code, rate_date, rate_value, create_time) VALUES (?, ?, ?, ?)";
            jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Object[] args = batchArgs.get(i);
                    ps.setString(1, (String) args[0]);
                    ps.setString(2, (String) args[1]);
                    ps.setBigDecimal(3, (BigDecimal) args[2]);
                    ps.setString(4, (String) args[3]); // 改为 setString 以匹配格式化后的时间
                }
                @Override
                public int getBatchSize() { return batchArgs.size(); }
            });
            log.info("成功通过 JDBC 批量保存 {} 条历史汇率记录", batchArgs.size());
        }
    }

    private String getAttributeValue(Element parent, String ns, String tagName, String attrName) {
        NodeList nodes = parent.getElementsByTagNameNS(ns, tagName);
        if (nodes.getLength() > 0) {
            return ((Element) nodes.item(0)).getAttribute(attrName);
        }
        return null;
    }

    /**
     * 查询历史汇率走势 (JDBC 实现)
     */
    public List<HistoryRateDto> getHistoryRates(String currency, String startDate, String endDate) {
        String sql = "SELECT rate_date, rate_value FROM exchange_rate_history WHERE currency_code = ? AND rate_date BETWEEN ? AND ? ORDER BY rate_date ASC";
        return jdbcTemplate.query(sql, historyRowMapper, currency, startDate, endDate);
    }
}