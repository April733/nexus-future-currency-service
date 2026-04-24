package com.nexusfuture.currency.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfuture.currency.entity.AiCallLog;
import com.nexusfuture.currency.repository.AiCallLogRepository;
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

    private final AiCallLogRepository logRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 🔥 修正：将 chat 改为 getChatCompletion，或者使用 * 通配符拦截所有以 Chat 开头的方法
    @Around("execution(* com.nexusfuture.currency.client.TongyiQwenClient.getChatCompletion*(..))")    public Object monitorAiCall(ProceedingJoinPoint joinPoint) throws Throwable {
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

            // 🔥 修复：增加 try-catch 保护，防止非 JSON 格式的返回值导致日志记录失败
            if (result != null && status.equals("SUCCESS")) {
                try {
                    String jsonStr = result.toString();
                    // 简单的判断：如果字符串不是以 { 或 [ 开头，大概率不是 JSON，直接跳过解析
                    if (jsonStr.trim().startsWith("{") || jsonStr.trim().startsWith("[")) {
                        JsonNode root = objectMapper.readTree(jsonStr);
                        JsonNode usage = root.path("usage");
                        if (!usage.isMissingNode()) {
                            logEntry.setInputTokens(usage.path("prompt_tokens").asInt(0));
                            logEntry.setOutputTokens(usage.path("completion_tokens").asInt(0));
                            logEntry.setTotalTokens(usage.path("total_tokens").asInt(0));
                        }
                    } else {
                        // 如果是纯文本，我们可以估算一下输出 Token (粗略按 1 token ≈ 2 字符计算)
                        logEntry.setOutputTokens(jsonStr.length() / 2);
                        logEntry.setTotalTokens(logEntry.getPromptLength() + logEntry.getOutputTokens());
                    }
                } catch (Exception e) {
                    // 即使 Token 解析失败，也不要影响主流程，只记录警告
                    log.warn("AI 响应非标准 JSON 格式，已跳过 Token 精确提取: {}", e.getMessage());
                }
            }

            logRepository.save(logEntry);
        } catch (Exception e) {
            log.error("记录 AI 监控日志失败", e);
        }
    }
}
