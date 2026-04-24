package com.nexusfuture.currency.controller;

import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.service.AiMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AiMonitorService aiMonitorService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        return Result.success(aiMonitorService.getOverview());
    }
}
