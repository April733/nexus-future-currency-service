package com.nexusfuture.currency.service;

import com.nexusfuture.currency.repository.AiCallLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiMonitorService {

    private final AiCallLogRepository logRepository;

    public Map<String, Object> getOverview() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // 1. 调用 Repository 获取统计结果
        dashboard.put("today", logRepository.getTodayStats());
        
        // 2. 获取最近日志
        dashboard.put("recentLogs", logRepository.findRecentLogs());
        
        return dashboard;
    }
}
