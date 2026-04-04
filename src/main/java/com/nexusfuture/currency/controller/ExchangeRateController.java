package com.nexusfuture.currency.controller;

import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.entity.ExchangeRate;
import com.nexusfuture.currency.repository.ExchangeRateRepository;
import com.nexusfuture.currency.service.ExchangeRateXmlService;
import com.nexusfuture.currency.util.XmlExchangeRateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rate")
public class ExchangeRateController {

    private final ExchangeRateRepository repository;
    private final ExchangeRateXmlService exchangeRateXmlService;

    @GetMapping("/convert")
    public Result<?> convert(
            @RequestParam String date,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "1") double amount
    ) {
        ExchangeRate rate = repository.findFirstByDateStartingWithOrderByIdDesc(date)
                .orElse(null);
        if (rate == null) {
            return Result.fail(400, "无当日汇率数据");
        }

        String xml = rate.getRawXml();
        double fromRate = XmlExchangeRateUtil.getRate(xml, from);
        double toRate = XmlExchangeRateUtil.getRate(xml, to);
        double result = amount * (toRate / fromRate);

        return Result.success(Map.of(
                "date", rate.getDate(),
                "from", from,
                "to", to,
                "amount", amount,
                "result", result,
                "url", rate.getUrl(),
                "source", "ECB欧洲央行（原始XML）"
        ));
    }

    /**
     * 根据已落库的 ECB XML，列出其中出现的货币及对应国家/地区（中英文）。
     *
     * @param date 日历日期 yyyy-MM-dd，缺省为当天；用于选取当日最后一次拉取的 XML
     */
    @GetMapping("/currencies")
    public Result<?> listCurrencies(
            @RequestParam(required = false) String date
    ) {
        String day = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
        ExchangeRate rate = repository.findFirstByDateStartingWithOrderByIdDesc(day)
                .orElse(null);
        if (rate == null) {
            return Result.fail(400, "无当日汇率数据，无法解析 XML");
        }
        return Result.success(Map.of(
                "date", rate.getDate(),
                "url", rate.getUrl(),
                "currencies", exchangeRateXmlService.listCurrencyCountries(rate.getRawXml())
        ));
    }
}