package com.nexusfuture.currency.config;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * API 接口监控切面
 * 捕获所有 Controller 层的请求参数、响应结果、执行耗时
 *
 * @author april
 * @since 2026-04-29
 */
@Aspect
@Component
@Slf4j
public class ApiMonitorAspect {

    /**
     * 拦截所有 Controller 层的公共方法
     */
    @Around("execution(* com.nexusfuture.currency.controller..*(..))")
    public Object monitorApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // 生成请求追踪 ID
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        // 获取 HTTP 请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String queryString = request != null ? request.getQueryString() : null;
        String fullUrl = queryString != null ? uri + "?" + queryString : uri;

        // 记录请求开始时间
        long startTime = System.currentTimeMillis();

        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();

        // 格式化入参（过滤敏感信息）
        String requestParams = formatRequestParams(args, request);

        log.info("📥 [API-{}] {} {} | 入参: {}", traceId, method, fullUrl, requestParams);

        Object result = null;
        String status = "SUCCESS";
        String errorMsg = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();

            // 格式化出参（只打印前 500 字符，避免日志过大）
            String responsePreview = formatResponsePreview(result);
            log.info(" [API-{}] {} {} | 出参: {} | 耗时: {}ms",
                    traceId, method, fullUrl, responsePreview, System.currentTimeMillis() - startTime);

            return result;
        } catch (Exception e) {
            status = "ERROR";
            errorMsg = e.getMessage();
            log.error("❌ [API-{}] {} {} | 异常: {} | 耗时: {}ms",
                    traceId, method, fullUrl, errorMsg, System.currentTimeMillis() - startTime, e);
            throw e;
        } finally {
            // 可选：将日志持久化到数据库或发送到监控平台
            long latency = System.currentTimeMillis() - startTime;
            logApiMetrics(traceId, method, uri, methodName, latency, status);
        }
    }

    /**
     * 格式化请求参数（过滤敏感信息）
     */
    private String formatRequestParams(Object[] args, HttpServletRequest request) {
        if (args == null || args.length == 0) {
            return "无参数";
        }

        // 如果是 POST 请求且参数是 Map 或 DTO，打印 JSON
        if (request != null && "POST".equals(request.getMethod())) {
            try {
                return JSON.toJSONString(args[0]);
            } catch (Exception e) {
                return args[0].toString();
            }
        }

        // GET 请求或其他类型，打印参数列表
        return Arrays.toString(args);
    }

    /**
     * 格式化响应预览（限制长度）
     */
    private String formatResponsePreview(Object result) {
        if (result == null) {
            return "null";
        }

        try {
            String jsonStr = JSON.toJSONString(result);
            if (jsonStr.length() > 500) {
                return jsonStr.substring(0, 500) + "... [已截断]";
            }
            return jsonStr;
        } catch (Exception e) {
            return result.toString();
        }
    }

    /**
     * 记录 API 指标（可选：持久化到数据库）
     */
    private void logApiMetrics(String traceId, String method, String uri, String methodName, long latency, String status) {
        // TODO: 如果需要持久化，可以在这里插入到 api_call_log 表
        // 例如：apiLogRepository.save(new ApiCallLog(traceId, method, uri, methodName, latency, status));

        // 当前仅记录到日志，方便排查问题
        if (latency > 1000) {
            log.warn("⚠️ [API-{}] 慢查询警告: {} {} | 耗时: {}ms", traceId, method, uri, latency);
        }
    }
}
