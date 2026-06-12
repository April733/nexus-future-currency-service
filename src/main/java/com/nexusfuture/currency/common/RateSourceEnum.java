package com.nexusfuture.currency.common;

/**
 * 汇率数据源类型枚举
 */
public enum RateSourceEnum {

    /**
     * 欧洲央行（European Central Bank）
     */
    ECB("ECB", "欧洲央行"),

    /**
     * 中国银行（Bank of China）
     */
    BOC("BOC", "中国银行");

    private final String code;
    private final String description;

    RateSourceEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码查找对应的枚举
     */
    public static RateSourceEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (RateSourceEnum source : values()) {
            if (source.getCode().equalsIgnoreCase(code)) {
                return source;
            }
        }
        return null;
    }
}
