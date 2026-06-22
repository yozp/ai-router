package com.yzj.airouter.service;

import com.yzj.airouter.model.dto.chat.ChatRequest;
import com.yzj.airouter.model.dto.chat.ChatResponse;
import com.yzj.airouter.model.dto.chat.StreamResponse;
import reactor.core.publisher.Flux;

public interface ChatService {

    /**
     * 非流式聊天
     */
    ChatResponse chat(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent);

    /**
     * 流式聊天
     */
    Flux<StreamResponse> chatStream(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent);
}
