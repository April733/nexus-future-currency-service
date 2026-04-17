package com.nexusfuture.currency.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    private String role;
    private String content;

    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    @JsonProperty("tool_call_id")
    private String toolCallId;

    private ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    private ChatMessage(String role, String content, String toolCallId) {
        this.role = role;
        this.content = content;
        this.toolCallId = toolCallId;
    }

    public static ChatMessage createUserMessage(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage createAssistantMessage(String content) {
        return new ChatMessage("assistant", content);
    }

    public static ChatMessage createToolMessage(String content, String toolCallId) {
        return new ChatMessage("tool", content, toolCallId);
    }
}