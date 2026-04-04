package com.nexusfuture.currency.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exchange_rate")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 拉取时间，格式 yyyy-MM-dd HH:mm:ss（同日多次拉取可区分） */
    private String date;

    @Column(length = 512)
    private String url;           // 保存请求的URL

    @Column(columnDefinition = "LONGTEXT")
    private String rawXml;        // 原始XML

    private LocalDateTime createTime;
}