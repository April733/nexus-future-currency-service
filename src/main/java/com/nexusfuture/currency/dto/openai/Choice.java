package com.nexusfuture.currency.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Choice {
    private int index;
    private ChatMessage message;
    private ChatMessage delta;

    @JsonProperty("finish_reason")
    private String finishReason;
}