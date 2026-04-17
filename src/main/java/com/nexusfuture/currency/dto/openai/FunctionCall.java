package com.nexusfuture.currency.dto.openai;

import lombok.Data;

@Data
public class FunctionCall {
    /**
     * The name of the function to be called.
     */
    private String name;

    /**
     * The arguments to call the function with, as provided by the model in JSON format (a string).
     */
    private String arguments;
}