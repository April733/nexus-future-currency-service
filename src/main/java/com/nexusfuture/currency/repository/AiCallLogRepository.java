package com.nexusfuture.currency.repository;

import com.nexusfuture.currency.entity.AiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {

    @Query("SELECT a FROM AiCallLog a ORDER BY a.createTime DESC LIMIT 50")
    List<AiCallLog> findRecentLogs();
}
