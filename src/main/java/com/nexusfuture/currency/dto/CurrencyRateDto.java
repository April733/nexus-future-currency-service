package com.nexusfuture.currency.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyRateDto implements Serializable {

    private String code;         // USD, CNY, THB...
    private String englishName;  // 国家英文
    private String chineseName;  // 国家中文
    private BigDecimal rate;     // 汇率
}