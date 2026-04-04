package com.nexusfuture.currency.dto;

/**
 * ECB XML 中出现的货币代码，及常用对应国家/地区中英文（展示用）。
 */
public record CurrencyCountryItem(String code, String countryEn, String countryZh) {}
