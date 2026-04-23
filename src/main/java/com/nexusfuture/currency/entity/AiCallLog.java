package com.nexusfuture.currency.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;

//CREATE TABLE IF NOT EXISTS ai_call_logs (
//        id INTEGER PRIMARY KEY AUTOINCREMENT,
//        model_name VARCHAR(50),           -- 模型名称 (e.g., qwen3.5-flash)
//prompt_length INTEGER,            -- 提示词长度
//input_tokens INTEGER,             -- 输入 Token 数
//output_tokens INTEGER,            -- 输出 Token 数
//total_tokens INTEGER,             -- 总 Token 数
//latency_ms INTEGER,               -- 耗时 (毫秒)
//status VARCHAR(20),               -- 状态: SUCCESS / ERROR
//error_msg TEXT,                   -- 错误信息 (如果失败)
//create_time TEXT                  -- 创建时间 (String 格式: yyyy-MM-dd HH:mm:ss)
//);

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_call_logs")
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "prompt_length")
    private Integer promptLength;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "status")
    private String status;

    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    // 关键点：使用 String 存储时间，保持与项目其他表一致
    @Column(name = "create_time")
    private String createTime;
}
