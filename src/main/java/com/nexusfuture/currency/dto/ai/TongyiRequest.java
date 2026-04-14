package com.nexusfuture.currency.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class TongyiRequest {
    private String model;
    private Input input;

    // 该内部类用于构造 "input" 字段
    @Data
    public static class Input {
        private List<Message> messages;
    }

    // 该内部类用于构造 "messages" 数组
    @Data
    public static class Message {
        private String role;
        private String content;
    }

    /**
     * 一个方便的静态方法，用于快速根据用户输入构建一个完整的请求对象
     *
     * @param userPrompt 用户输入的问题
     * @return 构造好的TongyiRequest对象
     */
    public static TongyiRequest createWithPrompt(String userPrompt) {
        TongyiRequest request = new TongyiRequest();
//        request.setModel("qwen-turbo"); // 使用通义千问的turbo模型
        request.setModel("qwen3.5-flash");


        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent(userPrompt);

        Input input = new Input();
        input.setMessages(List.of(userMessage));
        request.setInput(input);

        return request;
    }
}