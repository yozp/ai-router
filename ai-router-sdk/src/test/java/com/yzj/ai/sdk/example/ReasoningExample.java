package com.yzj.ai.sdk.example;

import com.yzj.ai.AIClient;
import com.yzj.ai.callback.StreamCallback;
import com.yzj.ai.model.ChatChunk;
import com.yzj.ai.model.ChatMessage;
import com.yzj.ai.model.ChatRequest;

import java.util.Arrays;

/**
 * 深度思考模式示例
 * 演示如何使用支持深度思考的模型（如 DeepSeek）
 */
public class ReasoningExample {

    public static void main(String[] args) throws InterruptedException {
        AIClient client = AIClient.builder()
                .apiKey("sk-xxx")
                .baseUrl("http://localhost:8127/api")
                .build();

        try {
            System.out.println("=== 深度思考模式示例 ===\n");

            // 构建启用深度思考的请求
            ChatRequest request = ChatRequest.builder()
                    .model("qwen-plus")  // 支持深度思考的模型
                    .messages(Arrays.asList(
                            ChatMessage.user("请详细解释量子纠缠现象，并给出实际应用")
                    ))
                    .enableReasoning(true)   // 启用深度思考
                    .temperature(0.7)
                    .build();

            client.chatStream(request, new StreamCallback() {
                @Override
                public void onMessage(ChatChunk chunk) {
                    // 分别处理思考内容和普通内容
                    if (chunk.getReasoningContent() != null) {
                        System.out.println("\n💭 [思考] " + chunk.getReasoningContent());
                    }

                    if (chunk.getContent() != null) {
                        System.out.print(chunk.getContent());
                    }
                }

                @Override
                public void onComplete() {
                    System.out.println("\n\n✅ 完成");
                }

                @Override
                public void onError(Throwable error) {
                    System.err.println("\n❌ 错误: " + error.getMessage());
                    error.printStackTrace();
                }
            });

            // 等待流式响应完成
            Thread.sleep(30000);

        } finally {
            client.close();
        }
    }
}