package com.nexusfuture.currency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillMonthlyPageRequest {

    private String yearMonth;  // 年月，格式：yyyy-MM，如 "2026-05"，为空则返回最新月份
    private Integer size;      // 返回几个月的数据，默认1
}
