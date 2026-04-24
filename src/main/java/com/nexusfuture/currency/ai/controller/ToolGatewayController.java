package com.nexusfuture.currency.ai.controller;

import com.nexusfuture.currency.ai.functioncall.ToolRegistry;
import com.nexusfuture.currency.common.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/tool")
@RequiredArgsConstructor
public class ToolGatewayController {

    private final ToolRegistry toolRegistry;

    @PostMapping("/execute")
    public Result<String> execute(@RequestBody ToolExecuteRequest request) {
        log.info("🚀 [ToolGateway] 收到工具执行请求: {}", request.getToolName());

        String result = toolRegistry.executeTool(
                request.getToolName(),
                request.getArgumentsJson()
        );

        // 🔥 修复：判断结果是否包含错误信息，返回对应的状态码
        if (result.contains("\"error\"") && result.contains("Tool not found")) {
            log.warn("工具不存在: {}", request.getToolName());
            return Result.fail(404, result);
        } else if (result.contains("\"error\"")) {
            log.error("工具执行失败: {}", result);
            return Result.fail(500, result);
        }

        return Result.success(result);
    }

    @Data
    public static class ToolExecuteRequest {
        private String toolName;
        private String argumentsJson;
    }
}
