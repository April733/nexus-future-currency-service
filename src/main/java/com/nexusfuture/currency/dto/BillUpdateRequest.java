package com.nexusfuture.currency.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BillUpdateRequest {

    private BigDecimal amount;
    private String currency;
    private String category;

    @Size(max = 10, message = "备注最多10个字符")
    private String remark;

    private String billDate;
    private Boolean status;
}
