package com.nexusfuture.currency.dto;

import java.math.BigDecimal;

public class HistoryRateDto {
    private String rateDate;
    private BigDecimal rateValue;

    // 确保有这个全参构造函数
    public HistoryRateDto(String rateDate, BigDecimal rateValue) {
        this.rateDate = rateDate;
        this.rateValue = rateValue;
    }

    // Getter 方法（Controller 返回 JSON 需要）
    public String getRateDate() { return rateDate; }
    public BigDecimal getRateValue() { return rateValue; }
}
