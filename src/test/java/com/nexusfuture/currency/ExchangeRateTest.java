package com.nexusfuture.currency;

import com.nexusfuture.currency.dto.CurrencyCountryItem;
import com.nexusfuture.currency.entity.ExchangeRate;
import com.nexusfuture.currency.repository.ExchangeRateRepository;
import com.nexusfuture.currency.scheduler.ExchangeRateScheduler;
import com.nexusfuture.currency.service.ExchangeRateXmlService;
import com.nexusfuture.currency.util.XmlExchangeRateUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
public class ExchangeRateTest {

    private final ExchangeRateRepository repository;
    private final ExchangeRateScheduler scheduler;
    private final ExchangeRateXmlService exchangeRateXmlService;

    @Autowired
    public ExchangeRateTest(
            ExchangeRateRepository repository,
            ExchangeRateScheduler scheduler,
            ExchangeRateXmlService exchangeRateXmlService
    ) {
        this.repository = repository;
        this.scheduler = scheduler;
        this.exchangeRateXmlService = exchangeRateXmlService;
    }

    @Test
    void testFetchAndSaveRate() {
        log.info("=== 开始测试：拉取欧洲央行XML并保存 ===");

        scheduler.fetch();

        String today = java.time.LocalDate.now().toString();
        ExchangeRate rate = repository.findFirstByDateStartingWithOrderByIdDesc(today)
                .orElseThrow(() -> new AssertionError("保存失败：当天无记录"));

        assertNotNull(rate, "保存失败：汇率对象为空");
        assertNotNull(rate.getRawXml(), "XML 内容为空");
        assertNotNull(rate.getUrl(), "URL 为空");

        log.info("✅ 测试通过！");
        log.info("日期：{}", rate.getDate());
        log.info("URL：{}", rate.getUrl());
        log.info("XML长度：{}", rate.getRawXml().length());
    }

    @Test
    void testParseAndCalculate() {
        log.info("=== 开始测试：解析XML + 计算 CNY->THB ===");

        testFetchAndSaveRate();

        String today = java.time.LocalDate.now().toString();
        ExchangeRate rate = repository.findFirstByDateStartingWithOrderByIdDesc(today)
                .orElseThrow(() -> new AssertionError("当天无记录"));
        String xml = rate.getRawXml();

        double cny = XmlExchangeRateUtil.getRate(xml, "CNY");
        double thb = XmlExchangeRateUtil.getRate(xml, "THB");
        double result = 100 * (thb / cny);

        log.info("1欧元 = {} 人民币", cny);
        log.info("1欧元 = {} 泰铢", thb);
        log.info("100人民币 = {} 泰铢", result);

        assertNotEquals(0, cny, "CNY 汇率为 0");
        assertNotEquals(0, thb, "THB 汇率为 0");
        log.info("✅ 解析与计算正常！");
    }

    @Test
    void testListCurrencyCountriesFromXml() {
        log.info("=== 开始测试：XML 服务列出货币及国家中英文 ===");

        testFetchAndSaveRate();

        String today = java.time.LocalDate.now().toString();
        ExchangeRate rate = repository.findFirstByDateStartingWithOrderByIdDesc(today)
                .orElseThrow(() -> new AssertionError("当天无记录"));
        var list = exchangeRateXmlService.listCurrencyCountries(rate.getRawXml());

        assertFalse(list.isEmpty(), "货币列表不应为空");
        CurrencyCountryItem cny = list.stream()
                .filter(i -> "CNY".equals(i.code()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("XML 中应包含 CNY"));
        assertEquals("China", cny.countryEn());
        assertEquals("中国", cny.countryZh());

        for (int i = 1; i < list.size(); i++) {
            assertTrue(
                    list.get(i - 1).code().compareTo(list.get(i).code()) <= 0,
                    "应按货币代码字母序排列");
        }

        log.info("✅ 共 {} 种货币", list.size());
        System.out.println("---- 货币列表（代码 | 英文国家/地区 | 中文）----");
        for (CurrencyCountryItem i : list) {
            System.out.println(i.code() + "\t" + i.countryEn() + "\t" + i.countryZh());
        }
    }
}