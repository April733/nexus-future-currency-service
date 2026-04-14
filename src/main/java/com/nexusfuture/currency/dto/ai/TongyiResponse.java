package com.nexusfuture.currency.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TongyiResponse {

    private Output output;

    @JsonProperty("request_id")
    private String requestId;

    // 该内部类用于解析 "output" 字段
    @Data
    public static class Output {
        private String text;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * 一个方便的辅助方法，用于直接获取最终生成的文本
     * @return AI生成的文本内容，如果不存在则返回null
     */
    public String getGeneratedText() {
        if (output != null && output.getText() != null) {
            return output.getText();
        }
        return null;
    }
}