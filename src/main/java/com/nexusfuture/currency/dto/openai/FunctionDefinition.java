package com.nexusfuture.currency.dto.openai;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class FunctionDefinition {
    private String name;
    private String description;
    private Map<String, Object> parameters;
}