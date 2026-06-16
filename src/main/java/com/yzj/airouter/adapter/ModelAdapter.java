package com.yzj.airouter.adapter;

import com.yzj.airouter.model.dto.chat.ChatRequest;
import com.yzj.airouter.model.dto.chat.StreamChunk;
import com.yzj.airouter.model.entity.Model;
import com.yzj.airouter.model.entity.ModelProvider;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * 模型适配器接口
 */
public interface ModelAdapter {

    /**
     * 同步调用模型
     */
    ChatResponse invoke(Model model, ModelProvider provider, ChatRequest chatRequest);

    /**
     * 流式调用模型（返回原始响应）
     */
    Flux<ChatResponse> invokeStream(Model model, ModelProvider provider, ChatRequest chatRequest);

    /**
     * 流式调用模型（返回统一格式的响应块，包含深度思考的内容）
     */
    Flux<StreamChunk> invokeStreamChunk(Model model, ModelProvider provider, ChatRequest chatRequest);

    /**
     * 判断当前适配器是否支持指定的提供商
     */
    boolean supports(String providerName);
}

