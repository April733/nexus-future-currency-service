package com.nexusfuture.currency.service;

import com.nexusfuture.currency.dto.CurrencyCountryItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析欧洲央行参考汇率 XML，提取货币代码并附国家/地区中英文名称。
 */
@Service
public class ExchangeRateXmlService {

    private static final Pattern CURRENCY_CODE =
            Pattern.compile("currency=['\"]([A-Z]{3})['\"]");

    /**
     * 从 ECB XML 中提取所有出现的 {@code currency="XXX"} 代码（去重、按字母序），
     * 并映射为主要使用国家/地区的中英文名称。
     */
    public List<CurrencyCountryItem> listCurrencyCountries(String xml) {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }
        Matcher m = CURRENCY_CODE.matcher(xml);
        var seen = new java.util.TreeSet<String>();
        while (m.find()) {
            seen.add(m.group(1));
        }
        List<CurrencyCountryItem> out = new ArrayList<>(seen.size());
        for (String code : seen) {
            out.add(new CurrencyCountryItem(
                    code,
                    CurrencyCountryNames.countryEn(code),
                    CurrencyCountryNames.countryZh(code)));
        }
        return out;
    }
}
