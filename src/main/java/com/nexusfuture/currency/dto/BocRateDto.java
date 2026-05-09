package com.nexusfuture.currency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BocRateDto {

    private String currencyCode;

    private String currencyName;

    private BigDecimal spotBuy;

    private BigDecimal cashBuy;

    private BigDecimal spotSell;

    private BigDecimal cashSell;

    private BigDecimal conversionRate;

    private String rateDate;

    private String publishTime;
}
