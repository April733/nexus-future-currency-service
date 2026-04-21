package com.nexusfuture.currency.ai.controller;

import com.nexusfuture.currency.ai.functioncall.ToolRegistry;
import com.nexusfuture.currency.common.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 工具网关控制器
 * 提供统一的 /api/tool/execute 入口，将外部请求转发给 ToolRegistry
 */
@Slf4j
@RestController
@RequestMapping("/api/tool")
@RequiredArgsConstructor
public class ToolGatewayController {

    private final ToolRegistry toolRegistry;

    @PostMapping("/execute")
    public Result<String> execute(@RequestBody ToolExecuteRequest request) {
        log.info("🚀 [ToolGateway] 收到工具执行请求: {}", request.getToolName());

        // 直接调用 ToolRegistry 已经写好的 executeTool 方法
        // 注意：ToolRegistry.executeTool 接收的是 JSON 字符串格式的 arguments
        String result = toolRegistry.executeTool(
                request.getToolName(),
                request.getArgumentsJson()
        );

        return Result.success(result);
    }

    @Data
    public static class ToolExecuteRequest {
        private String toolName;
        /**
         * 参数的 JSON 字符串形式
         * 例如: "{\"fromCurrency\":\"USD\",\"toCurrency\":\"CNY\"}"
         */
        private String argumentsJson;
    }
}
