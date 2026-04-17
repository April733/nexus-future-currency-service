package com.nexusfuture.currency.dto.openai;

import com.nexusfuture.currency.ai.tool.ExecutableTool;
import lombok.Data;

import java.util.Map;

@Data
public class Tool {
    private String type;
    private FunctionDefinition function;

    public Tool(ExecutableTool executableTool) {
        this.type = "function";
        this.function = new FunctionDefinition(
                executableTool.getName(),
                executableTool.getDescription(),
                executableTool.getParameters()
        );
    }
}
