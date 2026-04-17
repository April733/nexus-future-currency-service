package com.nexusfuture.currency.ai.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfuture.currency.dto.openai.Tool;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolRegistry {

    private final Map<String, ExecutableTool> toolMap = new ConcurrentHashMap<>();
    private final List<ExecutableTool> tools;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info("Initializing ToolRegistry...");
        for (ExecutableTool tool : tools) {
            log.info("Registering tool: {}", tool.getName());
            toolMap.put(tool.getName(), tool);
        }
        log.info("ToolRegistry initialized with {} tools.", toolMap.size());
    }

    public Optional<ExecutableTool> getTool(String name) {
        return Optional.ofNullable(toolMap.get(name));
    }

    public Collection<ExecutableTool> getAllTools() {
        return toolMap.values();
    }

    public List<Tool> getAllToolsForApi() {
        return getAllTools().stream()
                .map(Tool::new)
                .collect(Collectors.toList());
    }

    public String executeTool(String name, String argumentsJson) {
        ExecutableTool tool = toolMap.get(name);
        if (tool == null) {
            log.warn("Attempted to execute non-existent tool: {}", name);
            return "{\"error\": \"Tool not found: " + name + "\"}";
        }

        try {
            // 决定性修正：使用readValue来解析一个包含JSON的字符串
            Map<String, Object> arguments = objectMapper.readValue(argumentsJson, new TypeReference<>() {});
            log.info("Tool '{}' executed successfully with arguments: {}", name, argumentsJson);
            return tool.execute(arguments);
        } catch (Exception e) {
            log.error("Failed to parse/execute tool '{}' with arguments {}: {}", name, argumentsJson, e.getMessage(), e);
            return "{\"error\": \"Failed to execute tool " + name + ": " + e.getMessage() + "\"}";
        }

    }
}