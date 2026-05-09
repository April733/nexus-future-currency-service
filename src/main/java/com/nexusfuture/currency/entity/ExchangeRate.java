package com.nexusfuture.currency.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exchange_rate")
public class ExchangeRate {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("url")
    private String url;

    @TableField("raw_xml")
    private String rawXml;

    @TableField("create_time")
    private String createTime;
}