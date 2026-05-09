package com.nexusfuture.currency.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("boc_exchange_rate")
public class BocExchangeRate {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("currency_code")
    private String currencyCode;

    @TableField("currency_name")
    private String currencyName;

    @TableField("spot_buy")
    private BigDecimal spotBuy;

    @TableField("cash_buy")
    private BigDecimal cashBuy;

    @TableField("spot_sell")
    private BigDecimal spotSell;

    @TableField("cash_sell")
    private BigDecimal cashSell;

    @TableField("conversion_rate")
    private BigDecimal conversionRate;

    @TableField("rate_date")
    private String rateDate;

    @TableField("publish_time")
    private String publishTime;

    @TableField("create_time")
    private String createTime;
}
