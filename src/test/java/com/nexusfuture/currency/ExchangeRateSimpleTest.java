package com.nexusfuture.currency;

import com.nexusfuture.currency.entity.ExchangeRate;
import com.nexusfuture.currency.repository.ExchangeRateRepository;
import com.nexusfuture.currency.scheduler.ExchangeRateScheduler;
import com.nexusfuture.currency.util.XmlExchangeRateUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

public class ExchangeRateSimpleTest {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ExchangeServiceApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        ApplicationContext ctx = app.run(args);

        ExchangeRateScheduler scheduler = ctx.getBean(ExchangeRateScheduler.class);
        ExchangeRateRepository repository = ctx.getBean(ExchangeRateRepository.class);

        // 拉取并保存
        scheduler.fetch();

        // 取今天最新一条
        String today = java.time.LocalDate.now().toString();
        ExchangeRate rate = repository.findFirstByDateStartingWithOrderByIdDesc(today)
                .orElseThrow(() -> new IllegalStateException("当天无记录"));

        // 工具类解析
        double cny = XmlExchangeRateUtil.getRate(rate.getRawXml(), "CNY");
        double thb = XmlExchangeRateUtil.getRate(rate.getRawXml(), "THB");
        double result = 100 * (thb / cny);

        System.out.println("1 欧元 = " + cny + " 人民币");
        System.out.println("1 欧元 = " + thb + " 泰铢");
        System.out.println("100 人民币 ≈ " + String.format("%.2f", result) + " 泰铢");
    }
}