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
public class BillGroupedByDateResponse {

    private String date;
    private BigDecimal dailyTotal;
    private List<BillItemResponse> bills;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillItemResponse {
        private Long id;
        private BigDecimal amount;
        private String currency;
        private String category;
        private String remark;
        private String billDate;
        private Boolean status;
    }
}
