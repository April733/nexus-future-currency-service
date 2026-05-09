package com.nexusfuture.currency.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_call_logs")
public class AiCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("model_name")
    private String modelName;

    @TableField("prompt_length")
    private Integer promptLength;

    @TableField("input_tokens")
    private Integer inputTokens;

    @TableField("output_tokens")
    private Integer outputTokens;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("latency_ms")
    private Long latencyMs;

    @TableField("status")
    private String status;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("create_time")
    private String createTime;
}
