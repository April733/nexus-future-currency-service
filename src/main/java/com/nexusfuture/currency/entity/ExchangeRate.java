package com.nexusfuture.currency.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "exchange_rate")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", length = 512)
    private String url;           // 保存请求的URL

    @Column(name = "raw_xml", columnDefinition = "LONGTEXT")
    private String rawXml;        // 原始XML

    @Column(name = "create_time", length = 255)
    private String createTime;
}