package com.nexusfuture.currency.dto.openai;

import lombok.Data;

@Data
public class ToolCall {
    /**
     * The ID of the tool call.
     */
    private String id;

    /**
     * The type of the tool. For now, this is always "function".
     */
    private String type;

    /**
     * The function that the model wants to call.
     */
    private FunctionCall function;
}
