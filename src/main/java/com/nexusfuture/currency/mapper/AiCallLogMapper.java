package com.nexusfuture.currency.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusfuture.currency.entity.AiCallLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiCallLogMapper extends BaseMapper<AiCallLog> {

    /**
     * 查询最近 50 条日志
     */
    @Select("SELECT * FROM ai_call_logs ORDER BY create_time DESC LIMIT 50")
    List<AiCallLog> findRecentLogs();

    /**
     * 统计今日数据：调用次数、总Token、平均耗时
     */
    @Select("SELECT COUNT(*) as count, COALESCE(SUM(total_tokens), 0) as tokens, COALESCE(AVG(latency_ms), 0) as avg_latency FROM ai_call_logs WHERE DATE(create_time) = CURDATE()")
    Map<String, Object> getTodayStats();
}
