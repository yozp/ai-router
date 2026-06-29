package com.yzj.ai.sdk.example;

import com.yzj.ai.AIClient;
import com.yzj.ai.model.ChatMessage;
import com.yzj.ai.model.ChatRequest;
import com.yzj.ai.model.ChatResponse;

import java.util.Arrays;

/**
 * 高级功能示例
 */
public class AdvancedExample {

    public static void main(String[] args) {
        AIClient client = AIClient.builder()
                .apiKey("sk-xxx")
                .baseUrl("http://localhost:8127/api")
                .connectTimeout(15000)
                .readTimeout(60000)
                .maxRetries(5)
                .build();

        try {
            System.out.println("=== 多轮对话示例 ===\n");

            ChatRequest request = ChatRequest.builder()
                    .messages(Arrays.asList(
                            ChatMessage.system("你是一个编程助手"),
                            ChatMessage.user("什么是 Java？"),
                            ChatMessage.assistant("Java 是一种面向对象的编程语言..."),
                            ChatMessage.user("它的主要特点是什么？")
                    ))
                    .model("qwen-turbo")
                    .temperature(0.7)
                    .build();

            ChatResponse response = client.chat(request);
            System.out.println("回答: " + response.getContent());

        } finally {
            client.close();
        }
    }
}