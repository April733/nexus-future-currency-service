package com.nexusfuture.currency.service;

import com.nexusfuture.currency.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiMonitorService {

    private final AiCallLogMapper aiCallLogMapper;

    public Map<String, Object> getOverview() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // 1. 调用 Mapper 获取统计结果
        dashboard.put("today", aiCallLogMapper.getTodayStats());
        
        // 2. 获取最近日志
        dashboard.put("recentLogs", aiCallLogMapper.findRecentLogs());
        
        return dashboard;
    }
}
