package com.nexusfuture.currency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillGroupedPageResponse {

    private List<BillGroupedByDateResponse> groupedBills;
    private BigDecimal total;
    private String currentYearMonth;    // 当前返回的最新月份
    private String nextYearMonth;       // 下一页的月份（用于游标）
    private Boolean hasMore;            // 是否还有更多数据
}
