package com.nexusfuture.currency.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "exchange_rate_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"currency_code", "rate_date"}))
public class ExchangeRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency_code", length = 10, nullable = false)
    private String currencyCode;

    @Column(name = "rate_date", length = 20, nullable = false)
    private String rateDate;

    @Column(name = "rate_value", precision = 12, scale = 6, nullable = false)
    private BigDecimal rateValue;

    @Column(name = "create_time", length = 255)
    private String createTime;
}
