package com.nexusfuture.currency.repository;

import com.nexusfuture.currency.entity.AiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {

    @Query("SELECT a FROM AiCallLog a ORDER BY a.createTime DESC LIMIT 50")
    List<AiCallLog> findRecentLogs();

    /**
     * 统计今日数据：调用次数、总Token、平均耗时
     * 🔥 修改为 MySQL 语法
     */
    @Query(value = "SELECT COUNT(*) as count, COALESCE(SUM(total_tokens), 0) as tokens, COALESCE(AVG(latency_ms), 0) as avg_latency FROM ai_call_logs WHERE DATE(create_time) = CURDATE()", nativeQuery = true)
    Map<String, Object> getTodayStats();
}
