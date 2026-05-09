package com.nexusfuture.currency.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfuture.currency.entity.AiCallLog;
import com.nexusfuture.currency.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AiMonitorAspect {

    private final AiCallLogMapper aiCallLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("execution(* com.nexusfuture.currency.client.TongyiQwenClient.getChatCompletion*(..))")
    public Object monitorAiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("🚀 [AOP] 捕获到 AI 调用请求: {}", joinPoint.getSignature());
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMsg = null;
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            status = "ERROR";
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long latency = System.currentTimeMillis() - startTime;
            saveLog(joinPoint.getArgs(), result, latency, status, errorMsg);
        }
    }

    private void saveLog(Object[] args, Object result, long latency, String status, String errorMsg) {
        try {
            AiCallLog logEntry = new AiCallLog();
            logEntry.setModelName("qwen3.5-flash"); 
            logEntry.setPromptLength(args.length > 0 ? String.valueOf(args[0]).length() : 0);
            logEntry.setLatencyMs(latency);
            logEntry.setStatus(status);
            logEntry.setErrorMsg(errorMsg);
            logEntry.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            if (result != null && status.equals("SUCCESS")) {
                try {
                    String jsonStr = result.toString();
                    if (jsonStr.trim().startsWith("{") || jsonStr.trim().startsWith("[")) {
                        JsonNode root = objectMapper.readTree(jsonStr);
                        JsonNode usage = root.path("usage");
                        if (!usage.isMissingNode()) {
                            logEntry.setInputTokens(usage.path("prompt_tokens").asInt(0));
                            logEntry.setOutputTokens(usage.path("completion_tokens").asInt(0));
                            logEntry.setTotalTokens(usage.path("total_tokens").asInt(0));
                        }
                    } else {
                        logEntry.setOutputTokens(jsonStr.length() / 2);
                        logEntry.setTotalTokens(logEntry.getPromptLength() + logEntry.getOutputTokens());
                    }
                } catch (Exception e) {
                    log.warn("AI 响应非标准 JSON 格式，已跳过 Token 精确提取: {}", e.getMessage());
                }
            }

            aiCallLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("记录 AI 监控日志失败", e);
        }
    }
}
