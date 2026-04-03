package com.nexusfuture.currency.controller;

import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.entity.ExchangeRate;
import com.nexusfuture.currency.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rate")
public class ExchangeRateController {

    private final ExchangeRateRepository repository;

    @GetMapping("/convert")
    public Result<?> convert(
            @RequestParam String date,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "1") double amount
    ) {
        // 1. 从SQLite取出当天原始XML
        ExchangeRate rate = repository.findByDate(date);
        if (rate == null) {
            return Result.fail(400, "无当日汇率数据");
        }

        // 2. 用时才解析XML，计算汇率
        String xml = rate.getRawXml();
        double fromRate = getRate(xml, from);
        double toRate = getRate(xml, to);
        double result = amount * (toRate / fromRate);

        return Result.success(Map.of(
                "date", date,
                "from", from,
                "to", to,
                "amount", amount,
                "result", result,
                "url", rate.getUrl(),
                "source", "ECB欧洲央行（原始XML）"
        ));
    }

    // 从原始XML提取指定货币汇率
    private double getRate(String xml, String currency) {
        String key = "currency=\"" + currency + "\" rate=\"";
        int start = xml.indexOf(key) + key.length();
        int end = xml.indexOf("\"", start);
        return Double.parseDouble(xml.substring(start, end));
    }
}