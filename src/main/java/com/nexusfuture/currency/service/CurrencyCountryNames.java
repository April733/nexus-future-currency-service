package com.nexusfuture.currency.service;

import java.util.Map;

/**
 * ISO 4217 货币代码 → 主要使用国家/地区（英文 + 中文），用于 ECB 参考汇率列表展示。
 */
final class CurrencyCountryNames {

    private static final Map<String, String[]> BY_CODE = Map.ofEntries(
            Map.entry("AUD", arr("Australia", "澳大利亚")),
            Map.entry("BGN", arr("Bulgaria", "保加利亚")),
            Map.entry("BRL", arr("Brazil", "巴西")),
            Map.entry("CAD", arr("Canada", "加拿大")),
            Map.entry("CHF", arr("Switzerland", "瑞士")),
            Map.entry("CNY", arr("China", "中国")),
            Map.entry("CZK", arr("Czech Republic", "捷克")),
            Map.entry("DKK", arr("Denmark", "丹麦")),
            Map.entry("GBP", arr("United Kingdom", "英国")),
            Map.entry("HKD", arr("Hong Kong SAR", "中国香港")),
            Map.entry("HRK", arr("Croatia", "克罗地亚")),
            Map.entry("HUF", arr("Hungary", "匈牙利")),
            Map.entry("IDR", arr("Indonesia", "印度尼西亚")),
            Map.entry("ILS", arr("Israel", "以色列")),
            Map.entry("INR", arr("India", "印度")),
            Map.entry("ISK", arr("Iceland", "冰岛")),
            Map.entry("JPY", arr("Japan", "日本")),
            Map.entry("KRW", arr("South Korea", "韩国")),
            Map.entry("MXN", arr("Mexico", "墨西哥")),
            Map.entry("MYR", arr("Malaysia", "马来西亚")),
            Map.entry("NOK", arr("Norway", "挪威")),
            Map.entry("NZD", arr("New Zealand", "新西兰")),
            Map.entry("PHP", arr("Philippines", "菲律宾")),
            Map.entry("PLN", arr("Poland", "波兰")),
            Map.entry("RON", arr("Romania", "罗马尼亚")),
            Map.entry("RUB", arr("Russia", "俄罗斯")),
            Map.entry("SEK", arr("Sweden", "瑞典")),
            Map.entry("SGD", arr("Singapore", "新加坡")),
            Map.entry("THB", arr("Thailand", "泰国")),
            Map.entry("TRY", arr("Turkey", "土耳其")),
            Map.entry("USD", arr("United States", "美国")),
            Map.entry("ZAR", arr("South Africa", "南非")),
            Map.entry("EUR", arr("Euro area", "欧元区")));

    private CurrencyCountryNames() {}

    private static String[] arr(String en, String zh) {
        return new String[] {en, zh};
    }

    static String countryEn(String code) {
        String[] n = BY_CODE.get(code);
        return n != null ? n[0] : "Unknown (" + code + ")";
    }

    static String countryZh(String code) {
        String[] n = BY_CODE.get(code);
        return n != null ? n[1] : "未收录（" + code + "）";
    }
}
