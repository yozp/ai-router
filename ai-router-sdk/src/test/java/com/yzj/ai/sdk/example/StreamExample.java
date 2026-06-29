package com.yzj.ai.sdk.example;

import com.yzj.ai.AIClient;
import com.yzj.ai.callback.StreamCallback;
import com.yzj.ai.model.ChatChunk;

/**
 * 流式调用示例
 */
public class StreamExample {

    public static void main(String[] args) throws InterruptedException {
        AIClient client = AIClient.builder()
                .apiKey("sk-xxx")
                .baseUrl("http://localhost:8127/api")
                .build();

        try {
            System.out.println("开始流式调用...\n");

            client.chatStream("写一首关于春天的诗", new StreamCallback() {
                @Override
                public void onMessage(ChatChunk chunk) {
                    // 处理深度思考内容
                        if (chunk.getReasoningContent() != null) {
                        System.out.println("[思考] " + chunk.getReasoningContent());
                    }
                    // 处理普通文本内容
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
            Thread.sleep(10000);

        } finally {
            client.close();
        }
    }
}