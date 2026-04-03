package com.nexusfuture.currency;

import com.nexusfuture.currency.entity.ExchangeRate;
import com.nexusfuture.currency.repository.ExchangeRateRepository;
import com.nexusfuture.currency.scheduler.ExchangeRateScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ExchangeRateTest {

    @Autowired
    private ExchangeRateRepository repository;

    @Autowired
    private ExchangeRateScheduler scheduler;

    @Test
    void testFetchAndSaveRate() {
        System.out.println("=== 开始测试：拉取欧洲央行XML并保存 ===");

        scheduler.fetch();

        String today = java.time.LocalDate.now().toString();
        ExchangeRate rate = repository.findByDate(today);

        assertNotNull(rate, "保存失败：汇率对象为空");
        assertNotNull(rate.getRawXml(), "XML 内容为空");
        assertNotNull(rate.getUrl(), "URL 为空");

        System.out.println("✅ 测试通过！");
        System.out.println("日期：" + rate.getDate());
        System.out.println("URL：" + rate.getUrl());
        System.out.println("XML长度：" + rate.getRawXml().length());
    }

    @Test
    void testParseAndCalculate() {
        System.out.println("=== 开始测试：解析XML + 计算 CNY->THB ===");

        // 先确保数据存在
        testFetchAndSaveRate();

        String today = java.time.LocalDate.now().toString();
        ExchangeRate rate = repository.findByDate(today);
        String xml = rate.getRawXml();

        double cny = getRate(xml, "CNY");
        double thb = getRate(xml, "THB");
        double result = 100 * (thb / cny);

        System.out.println("1欧元 = " + cny + " 人民币");
        System.out.println("1欧元 = " + thb + " 泰铢");
        System.out.println("100人民币 = " + result + " 泰铢");

        assertNotEquals(0, cny, "CNY 汇率为 0");
        assertNotEquals(0, thb, "THB 汇率为 0");
        System.out.println("✅ 解析与计算正常！");
    }

    private double getRate(String xml, String currency) {
        String key = "currency=\"" + currency + "\" rate=\"";
        int start = xml.indexOf(key) + key.length();
        int end = xml.indexOf("\"", start);
        return Double.parseDouble(xml.substring(start, end));
    }
}