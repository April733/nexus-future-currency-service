package com.nexusfuture.currency.common;

/**
 * ISO 4217 货币及其主要使用国家/地区信息的枚举。
 * <p>
 * 使用枚举可以提供编译时类型安全，并使代码更具可读性和健壮性。
 */
public enum CurrencyInfoEnum {

    AUD("Australia", "澳大利亚"),
    BGN("Bulgaria", "保加利亚"),
    BRL("Brazil", "巴西"),
    CAD("Canada", "加拿大"),
    CHF("Switzerland", "瑞士"),
    CNY("China", "中国"),
    CZK("Czech Republic", "捷克"),
    DKK("Denmark", "丹麦"),
    GBP("United Kingdom", "英国"),
    HKD("Hong Kong SAR", "中国香港"),
    HRK("Croatia", "克罗地亚"),
    HUF("Hungary", "匈牙利"),
    IDR("Indonesia", "印度尼西亚"),
    ILS("Israel", "以色列"),
    INR("India", "印度"),
    ISK("Iceland", "冰岛"),
    JPY("Japan", "日本"),
    KRW("South Korea", "韩国"),
    MXN("Mexico", "墨西哥"),
    MYR("Malaysia", "马来西亚"),
    NOK("Norway", "挪威"),
    NZD("New Zealand", "新西兰"),
    PHP("Philippines", "菲律宾"),
    PLN("Poland", "波兰"),
    RON("Romania", "罗马尼亚"),
    RUB("Russia", "俄罗斯"),
    SEK("Sweden", "瑞典"),
    SGD("Singapore", "新加坡"),
    THB("Thailand", "泰国"),
    TRY("Turkey", "土耳其"),
    USD("United States", "美国"),
    ZAR("South Africa", "南非"),
    EUR("Euro area", "欧元区");

    private final String englishName;
    private final String chineseName;

    CurrencyInfoEnum(String englishName, String chineseName) {
        this.englishName = englishName;
        this.chineseName = chineseName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getChineseName() {
        return chineseName;
    }

    /**
     * 根据货币代码（不区分大小写）查找对应的枚举实例。
     *
     * @param code ISO 4217 货币代码字符串
     * @return 如果找到，则返回对应的 {@link CurrencyInfoEnum} 实例；否则返回 {@code null}。
     */
    public static CurrencyInfoEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        try {
            return CurrencyInfoEnum.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // 未在枚举中定义
        }
    }

    /**
     * 根据货币代码获取对应的国家/地区英文名称。
     *
     * @param code ISO 4217 货币代码
     * @return 如果找到，返回英文名称；否则返回一个表示未知的默认字符串。
     */
    public static String getEnglishNameByCode(String code) {
        CurrencyInfoEnum info = fromCode(code);
        return info != null ? info.getEnglishName() : "Unknown (" + code + ")";
    }

    /**
     * 根据货币代码获取对应的国家/地区中文名称。
     *
     * @param code ISO 4217 货币代码
     * @return 如果找到，返回中文名称；否则返回一个表示未收录的默认字符串。
     */
    public static String getChineseNameByCode(String code) {
        CurrencyInfoEnum info = fromCode(code);
        return info != null ? info.getChineseName() : "未收录（" + code + "）";
    }
}
