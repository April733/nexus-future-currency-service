package com.nexusfuture.currency.ai.functioncall;

import java.util.Map;

public interface ExecutableTool {
    String getName();
    String getDescription();
    Map<String, Object> getParameters();
    String execute(Map<String, Object> arguments);
}
