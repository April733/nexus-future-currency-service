package com.nexusfuture.currency.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("bill")
public class Bill {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("currency")
    private String currency;

    @TableField("category")
    private String category;

    @TableField("remark")
    private String remark;

    @TableField("bill_date")
    private String billDate;

    @TableField("status")
    private Boolean status;

    @TableField("is_deleted")
    private Boolean isDeleted;

    @TableField("create_time")
    private String createTime;

    @TableField("update_time")
    private String updateTime;
}
