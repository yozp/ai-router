package com.yzj.airouter;

import com.alibaba.cloud.ai.autoconfigure.dashscope.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        // 排除 OpenAI 自动配置
        OpenAiChatAutoConfiguration.class,
        OpenAiEmbeddingAutoConfiguration.class,
        OpenAiImageAutoConfiguration.class,
        OpenAiAudioSpeechAutoConfiguration.class,
        OpenAiAudioTranscriptionAutoConfiguration.class,
        OpenAiModerationAutoConfiguration.class,
        // 排除 DeepSeek 自动配置
        DeepSeekChatAutoConfiguration.class,
        // 排除 DashScope 自动配置，因为我们都是给每个请求创建一个 chatModel，而不是使用 SpringAI Alibaba 自动注入的 chatModel
        DashScopeChatAutoConfiguration.class,
        DashScopeAgentAutoConfiguration.class,
        DashScopeEmbeddingAutoConfiguration.class,
        DashScopeImageAutoConfiguration.class,
        DashScopeAudioSpeechAutoConfiguration.class,
        DashScopeAudioTranscriptionAutoConfiguration.class,
        DashScopeRerankAutoConfiguration.class,
        DashScopeVideoAutoConfiguration.class
})
@MapperScan("com.yzj.airouter.mapper") //扫描 Mapper 文件
@EnableAsync
@EnableScheduling
public class AiRouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRouterApplication.class, args);
    }

}
