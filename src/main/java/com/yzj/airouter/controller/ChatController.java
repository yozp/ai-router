package com.yzj.airouter.controller;

import com.yzj.airouter.exception.BusinessException;
import com.yzj.airouter.exception.ErrorCode;
import com.yzj.airouter.model.dto.chat.ChatRequest;
import com.yzj.airouter.model.entity.ApiKey;
import com.yzj.airouter.service.ApiKeyService;
import com.yzj.airouter.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 外用于外部 API 调用
 */
@RestController
@RequestMapping("/v1/chat")
@Slf4j
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private ApiKeyService apiKeyService;

    /**
     * Chat Completions 接口
     * 支持流式和非流式响应
     */
    @PostMapping(value = "/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    @Operation(summary = "Chat Completions")
    public Object chatCompletions(@RequestBody ChatRequest request,
                                  @RequestHeader(value = "Authorization", required = false) String authorization,
                                  HttpServletRequest httpRequest) {
        // 1. 验证 API Key
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "缺少或无效的 Authorization Header");
        }

        String apiKeyValue = authorization.substring(7);
        ApiKey apiKey = apiKeyService.getByKeyValue(apiKeyValue);

        if (apiKey == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "API Key 无效或已失效");
        }

        // 2. 参数校验
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "messages 不能为空");
        }

        // 3. 设置默认模型
        if (request.getModel() == null || request.getModel().isEmpty()) {
            request.setModel("qwen-plus");
        }

        // 4. 判断是否为流式请求
        Boolean stream = request.getStream();
        if (stream != null && stream) {
            // 流式响应
            return chatService.chatStream(request, apiKey.getUserId(), apiKey.getId());
        } else {
            // 非流式响应
            return chatService.chat(request, apiKey.getUserId(), apiKey.getId());
        }
    }
}
