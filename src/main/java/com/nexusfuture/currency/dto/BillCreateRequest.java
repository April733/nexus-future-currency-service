package com.nexusfuture.currency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BillCreateRequest {

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @NotBlank(message = "货币代码不能为空")
    private String currency;

    @NotBlank(message = "分类不能为空")
    private String category;

    @Size(max = 10, message = "备注最多10个字符")
    private String remark;

    @NotBlank(message = "日期不能为空")
    private String billDate;
}
