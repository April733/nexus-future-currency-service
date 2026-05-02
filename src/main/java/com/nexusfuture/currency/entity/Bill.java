package com.nexusfuture.currency.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "bill")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String userId;  // 绑定用户

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;  // 金额

    @Column(nullable = false, length = 10)
    private String currency;  // 货币代码：CNY/USD/EUR

    @Column(nullable = false, length = 20)
    private String category;  // 分类：餐饮/住宿/交通/购物/其他

    @Column(length = 10)
    private String remark;  // 备注，最多10字符

    @Column(nullable = false, length = 20)
    private String billDate;  // 账单日期：2026-05-02 或 2026-05-02 12:07:11

    @Column(nullable = false)
    private Boolean status = true;  // 状态：true-启用，false-关闭

    @Column(nullable = false)
    private Boolean isDeleted = false;  // 逻辑删除

    @Column(length = 255)
    private String createTime;

    @Column(length = 255)
    private String updateTime;
}
