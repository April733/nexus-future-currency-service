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
public class BillResponse {

    private Long id;
    private BigDecimal amount;
    private String currency;
    private String category;          // 对应前端的 categoryDisplayText
    private String remark;
    private String billDate;          // 对应前端的 dateDisplayText
    private Boolean status;
    private String createTime;
}
