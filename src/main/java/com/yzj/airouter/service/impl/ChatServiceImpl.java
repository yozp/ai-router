package com.yzj.airouter.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.yzj.airouter.exception.BusinessException;
import com.yzj.airouter.exception.ErrorCode;
import com.yzj.airouter.metrics.AIMetricsCollector;
import com.yzj.airouter.model.dto.chat.*;
import com.yzj.airouter.model.dto.log.RequestLogDTO;
import com.yzj.airouter.model.entity.Model;
import com.yzj.airouter.model.entity.ModelProvider;
import com.yzj.airouter.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

//    @Resource
//    private ChatModel chatModel;

    @Resource
    private RequestLogService requestLogService;

    @Resource
    private RoutingService routingService;

    @Resource
    private ModelInvokeService modelInvokeService;

    @Resource
    private ModelProviderService modelProviderService;

    @Resource
    private UserProviderKeyService userProviderKeyService;

    @Resource
    private AIMetricsCollector aiMetricsCollector;

    @Resource
    private QuotaService quotaService;

    @Resource
    private BillingService billingService;

    @Resource
    private BalanceService balanceService;

    @Resource
    private UserService userService;

    /**
     * 最大 Fallback 重试次数
     */
    private static final int MAX_FALLBACK_RETRIES = 3;

    @Override
    public ChatResponse chat(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent) {
        long startTime = System.currentTimeMillis();
        String requestedModel = chatRequest.getModel();
        String traceId = IdUtil.simpleUUID();

        // 1. 检查用户状态
        if (userId != null && userService.isUserDisabled(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被禁用，无法使用服务");
        }

        // 2. 检查用户配额
        if (userId != null && !quotaService.checkQuota(userId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Token配额已用尽，请联系管理员增加配额");
        }

        // 确定路由策略：优先使用请求中指定的策略，否则根据是否指定模型决定
        String strategyType = determineStrategyType(chatRequest.getRoutingStrategy(), requestedModel);

        // 选择模型
        Model selectedModel = routingService.selectModel(strategyType, "chat", requestedModel);

        if (selectedModel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有可用的模型");
        }

        // 获取 Fallback 模型列表
        List<Model> fallbackModels = routingService.getFallbackModels(strategyType, "chat", requestedModel);

        // 尝试调用主模型
        boolean isFallback = false;

        try {
            return invokeModelWithFallback(selectedModel, fallbackModels, chatRequest,
                    userId, apiKeyId, traceId, startTime, strategyType, isFallback, clientIp, userAgent);
        } catch (Exception e) {
            log.error("所有模型调用失败", e);
            long duration = System.currentTimeMillis() - startTime;
            // 记录失败日志
            requestLogService.logRequest(RequestLogDTO.builder()
                    .traceId(traceId)
                    .userId(userId)
                    .apiKeyId(apiKeyId)
                    .requestModel(requestedModel)
                    .requestType("chat")
                    .source(apiKeyId != null ? "api" : "web")
                    .duration((int) duration)
                    .status("failed")
                    .errorMessage(e.getMessage())
                    .errorCode("SYSTEM_ERROR")
                    .routingStrategy(strategyType)
                    .isFallback(false)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .build());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用模型失败: " + e.getMessage());
        }
    }

    /**
     * 确定路由策略类型
     *
     * @param requestedStrategy 请求中指定的策略
     * @param requestedModel    请求中指定的模型
     * @return 最终使用的策略类型
     */
    private String determineStrategyType(String requestedStrategy, String requestedModel) {
        // 如果请求中指定了策略，优先使用
        if (StrUtil.isNotBlank(requestedStrategy)) {
            return requestedStrategy;
        }
        // 如果指定了模型但没有指定策略，使用固定模型策略
        if (StrUtil.isNotBlank(requestedModel)) {
            return "fixed";
        }
        // 默认使用自动路由策略
        return "auto";
    }

    /**
     * 带 Fallback 的模型调用
     */
    private ChatResponse invokeModelWithFallback(Model primaryModel, List<Model> fallbackModels,
                                                 ChatRequest chatRequest, Long userId, Long apiKeyId,
                                                 String traceId, long startTime, String strategyType, boolean isFallback,
                                                 String clientIp, String userAgent) {
        // 尝试调用主模型
        try {
            log.info("调用模型，{}", primaryModel.getModelKey());
            return callModel(primaryModel, chatRequest, userId, apiKeyId, traceId, startTime, strategyType, isFallback, clientIp, userAgent);
        } catch (Exception e) {
            log.warn("模型 {} 调用失败，尝试 Fallback", primaryModel.getModelKey(), e);

            // 如果主模型失败且有 Fallback 模型，尝试 Fallback
            if (fallbackModels != null && !fallbackModels.isEmpty()) {
                int retries = Math.min(fallbackModels.size(), MAX_FALLBACK_RETRIES);
                for (int i = 0; i < retries; i++) {
                    Model fallbackModel = fallbackModels.get(i);
                    try {
                        log.info("尝试 Fallback 模型: {}", fallbackModel.getModelKey());
                        return callModel(fallbackModel, chatRequest, userId, apiKeyId, traceId, startTime, strategyType, true, clientIp, userAgent);
                    } catch (Exception fallbackException) {
                        log.warn("Fallback 模型 {} 调用失败", fallbackModel.getModelKey(), fallbackException);
                        if (i == retries - 1) {
                            throw fallbackException;
                        }
                    }
                }
            }
            throw e;
        }
    }

    /**
     * 调用单个模型
     */
    private ChatResponse callModel(Model model, ChatRequest chatRequest, Long userId, Long apiKeyId,
                                   String traceId, long startTime, String strategyType, boolean isFallback,
                                   String clientIp, String userAgent) {
        // 获取提供者信息
        ModelProvider provider = modelProviderService.getById(model.getProviderId());
        if (provider == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模型提供者不存在");
        }

        // 检查用户是否配置了 BYOK（用户自带密钥）
        boolean isByok = false;
        if (userId != null) {
            String userApiKey = userProviderKeyService.getUserProviderApiKey(userId, model.getProviderId());
            if (userApiKey != null) {
                // 使用用户自己的密钥（BYOK 模式）
                provider = ModelProvider.builder()
                        .id(provider.getId())
                        .providerName(provider.getProviderName())
                        .displayName(provider.getDisplayName())
                        .baseUrl(provider.getBaseUrl())
                        // 使用用户的密钥
                        .apiKey(userApiKey)
                        .status(provider.getStatus())
                        .priority(provider.getPriority())
                        .build();
                isByok = true;
                log.info("用户 {} 使用 BYOK 模式调用模型 {}", userId, model.getModelKey());
            }
        }

        try {
            // 调用模型
            org.springframework.ai.chat.model.ChatResponse aiResponse =
                    modelInvokeService.invoke(model, provider, chatRequest);

            // 转换响应格式
            ChatResponse response = convertResponse(aiResponse, model.getModelKey());

            // 记录请求日志
            long duration = System.currentTimeMillis() - startTime;
            int totalTokens = response.getUsage().getTotalTokens();
            requestLogService.logRequest(RequestLogDTO.builder()
                    .traceId(traceId)
                    .userId(userId)
                    .apiKeyId(apiKeyId)
                    .modelId(model.getId())
                    .requestModel(model.getModelKey())
                    .requestType("chat")
                    .source(apiKeyId != null ? "api" : "web")
                    .promptTokens(response.getUsage().getPromptTokens())
                    .completionTokens(response.getUsage().getCompletionTokens())
                    .totalTokens(totalTokens)
                    .duration((int) duration)
                    .status("success")
                    .routingStrategy(strategyType)
                    .isFallback(isFallback)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .build());

            // 收集监控指标
            aiMetricsCollector.recordRequest(model.getModelKey(), userId, apiKeyId != null ? apiKeyId.toString() : null);
            aiMetricsCollector.recordTokens(model.getModelKey(), totalTokens);
            aiMetricsCollector.recordResponseTime(model.getModelKey(), duration);

            // 扣减用户配额和余额（BYOK 模式下免费，不扣减）
            if (userId != null && totalTokens > 0 && !isByok) {
                quotaService.deductTokens(userId, totalTokens);

                // 计算费用并扣减余额
                java.math.BigDecimal cost = billingService.calculateCost(
                        model,
                        response.getUsage().getPromptTokens(),
                        response.getUsage().getCompletionTokens()
                );

                if (cost.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    // 根据来源区分描述
                    String description = apiKeyId != null
                            ? "API调用消费 - " + model.getModelKey()
                            : "网页调用消费 - " + model.getModelKey();
                    balanceService.deductBalance(userId, cost, null, description);
                }
            } else if (isByok) {
                log.info("BYOK 模式：用户 {} 使用自己的密钥，不扣减余额和配额", userId);
            }

            // 缓存响应
            // cacheService.cacheResponse(chatRequest, response);

            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            // 记录失败日志
            requestLogService.logRequest(RequestLogDTO.builder()
                    .traceId(traceId)
                    .userId(userId)
                    .apiKeyId(apiKeyId)
                    .modelId(model.getId())
                    .requestModel(model.getModelKey())
                    .requestType("chat")
                    .source(apiKeyId != null ? "api" : "web")
                    .duration((int) duration)
                    .status("failed")
                    .errorMessage(e.getMessage())
                    .errorCode("MODEL_ERROR")
                    .routingStrategy(strategyType)
                    .isFallback(isFallback)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .build());

            // 收集错误指标
            aiMetricsCollector.recordError(model.getModelKey(), "MODEL_ERROR");

            throw e;
        }
    }

    /**
     * 构建 Prompt
     */
    private Prompt buildPrompt(ChatRequest chatRequest) {
        List<Message> messages = chatRequest.getMessages().stream()
                .map(msg -> switch (msg.getRole()) {
                    case "system" -> new SystemMessage(msg.getContent());
                    case "assistant" -> new AssistantMessage(msg.getContent());
                    default -> new UserMessage(msg.getContent());
                })
                .collect(Collectors.toList());

        // 构建选项
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(chatRequest.getModel())
                // 启用流式响应的 token 统计
                .streamUsage(true);

        if (chatRequest.getTemperature() != null) {
            optionsBuilder.temperature(chatRequest.getTemperature());
        }
        if (chatRequest.getMaxTokens() != null) {
            optionsBuilder.maxTokens(chatRequest.getMaxTokens());
        }

        return new Prompt(messages, optionsBuilder.build());
    }

    /**
     * 转换响应格式
     */
    private ChatResponse convertResponse(org.springframework.ai.chat.model.ChatResponse aiResponse, String modelName) {
        String content = aiResponse.getResult().getOutput().getText();

        ChatResponse.Usage usage = ChatResponse.Usage.builder()
                .promptTokens(aiResponse.getMetadata().getUsage().getPromptTokens() != null ?
                        aiResponse.getMetadata().getUsage().getPromptTokens() : 0)
                .completionTokens(aiResponse.getMetadata().getUsage().getCompletionTokens() != null ?
                        aiResponse.getMetadata().getUsage().getCompletionTokens() : 0)
                .totalTokens(aiResponse.getMetadata().getUsage().getTotalTokens() != null ?
                        aiResponse.getMetadata().getUsage().getTotalTokens() : 0)
                .build();

        ChatResponse.Choice choice = ChatResponse.Choice.builder()
                .index(0)
                .message(new ChatMessage("assistant", content))
                .finishReason(aiResponse.getResult().getMetadata().getFinishReason())
                .build();

        return ChatResponse.builder()
                .id(IdUtil.simpleUUID())
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model(modelName)
                .choices(List.of(choice))
                .usage(usage)
                .build();
    }

    //-------------------------------------------------------------------------------------------------------------------


    @Override
    public Flux<StreamResponse> chatStream(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent) {
        String requestedModel = chatRequest.getModel();
        long startTime = System.currentTimeMillis();
        String traceId = IdUtil.simpleUUID();
        long created = System.currentTimeMillis() / 1000;

        // 检查用户状态
        if (userId != null && userService.isUserDisabled(userId)) {
            return Flux.error(new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被禁用，无法使用服务"));
        }

//        // 如果指定了插件，先执行插件获取上下文，然后注入到消息中
//        if (StrUtil.isNotBlank(chatRequest.getPluginKey())) {
//            chatRequest = injectPluginContext(chatRequest, userId);
//        }

        // Token 计数器
        final int[] promptTokens = {0};
        final int[] completionTokens = {0};
        final boolean[] isFirstChunk = {true};

        try {
            // 确定路由策略：优先使用请求中指定的策略，否则根据是否指定模型决定
            String strategyType = determineStrategyType(chatRequest.getRoutingStrategy(), requestedModel);

            // 选择模型（策略内部查询数据库）
            Model selectedModel = routingService.selectModel(strategyType, "chat", requestedModel);

            if (selectedModel == null) {
                return Flux.error(new BusinessException(ErrorCode.PARAMS_ERROR, "没有可用的模型"));
            }

            // 获取提供者信息
            ModelProvider provider = modelProviderService.getById(selectedModel.getProviderId());
            if (provider == null) {
                return Flux.error(new BusinessException(ErrorCode.PARAMS_ERROR, "模型提供者不存在"));
            }

            // 检查用户是否配置了 BYOK（用户自带密钥）
            final boolean[] isByok = {false};
            if (userId != null) {
                String userApiKey = userProviderKeyService.getUserProviderApiKey(userId, selectedModel.getProviderId());
                if (userApiKey != null) {
                    // 使用用户自己的密钥（BYOK 模式）
                    provider = ModelProvider.builder()
                            .id(provider.getId())
                            .providerName(provider.getProviderName())
                            .displayName(provider.getDisplayName())
                            .baseUrl(provider.getBaseUrl())
                            .apiKey(userApiKey)  // 使用用户的密钥
                            .status(provider.getStatus())
                            .priority(provider.getPriority())
                            .build();
                    isByok[0] = true;
                    log.info("用户 {} 使用 BYOK 模式调用流式模型 {}", userId, selectedModel.getModelKey());
                }
            }

            // BYOK 模式下不检查配额和余额
            if (!isByok[0]) {
                // 检查用户配额
                if (userId != null && !quotaService.checkQuota(userId)) {
                    return Flux.error(new BusinessException(ErrorCode.OPERATION_ERROR, "Token配额已用尽，请联系管理员增加配额"));
                }

                // 检查用户余额（预估检查）
                if (userId != null) {
                    java.math.BigDecimal currentBalance = balanceService.getUserBalance(userId);
                    if (currentBalance.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                        return Flux.error(new BusinessException(ErrorCode.OPERATION_ERROR,
                                "账户余额不足，当前余额：¥" + currentBalance + "，请先充值"));
                    }
                }
            } else {
                log.info("BYOK 模式：跳过余额和配额检查");
            }

            // 调用流式模型，获取统一格式的响应块
            Flux<StreamChunk> chunkFlux = modelInvokeService.invokeStreamChunk(selectedModel, provider, chatRequest);

            // 将统一格式的响应块转换为 OpenAI SSE 格式的 StreamResponse
            return chunkFlux.flatMap(chunk -> {
                        // 更新 Token 统计
                        if (chunk.getPromptTokens() != null && chunk.getPromptTokens() > 0) {
                            promptTokens[0] = chunk.getPromptTokens();
                        }
                        if (chunk.getCompletionTokens() != null && chunk.getCompletionTokens() > 0) {
                            completionTokens[0] = chunk.getCompletionTokens();
                        }

                        // 构建 Delta
                        StreamResponse.Delta.DeltaBuilder deltaBuilder = StreamResponse.Delta.builder();

                        // 第一个块包含 role
                        if (isFirstChunk[0]) {
                            deltaBuilder.role("assistant");
                            isFirstChunk[0] = false;
                        }

                        // 处理普通文本内容
                        if (chunk.hasText()) {
                            deltaBuilder.content(chunk.getText());
                        }

                        // 处理深度思考内容
                        if (chunk.hasReasoningContent()) {
                            deltaBuilder.reasoningContent(chunk.getReasoningContent());
                        }

                        // 如果既没有文本也没有思考内容，跳过
                        if (!chunk.hasText() && !chunk.hasReasoningContent()) {
                            return Flux.empty();
                        }

                        StreamResponse.Delta delta = deltaBuilder.build();

                        StreamResponse.StreamChoice choice = StreamResponse.StreamChoice.builder()
                                .index(0)
                                .delta(delta)
                                .finishReason(null)
                                .build();

                        return Flux.just(StreamResponse.builder()
                                .id(traceId)
                                .object("chat.completion.chunk")
                                .created(created)
                                .model(selectedModel.getModelKey())
                                .choices(List.of(choice))
                                .build());
                    })
                    // 在流结束时追加一个带 finishReason: "stop" 的结束标识
                    .concatWith(Flux.defer(() -> {
                        StreamResponse.StreamChoice finishChoice = StreamResponse.StreamChoice.builder()
                                .index(0)
                                .delta(StreamResponse.Delta.builder().build())
                                .finishReason("stop")
                                .build();
                        return Flux.just(StreamResponse.builder()
                                .id(traceId)
                                .object("chat.completion.chunk")
                                .created(created)
                                .model(selectedModel.getModelKey())
                                .choices(List.of(finishChoice))
                                .build());
                    }))
                    .doOnComplete(() -> {
                        // 流结束时记录日志
                        long duration = System.currentTimeMillis() - startTime;
                        int totalTokens = promptTokens[0] + completionTokens[0];
                        requestLogService.logRequest(RequestLogDTO.builder()
                                .traceId(traceId)
                                .userId(userId)
                                .apiKeyId(apiKeyId)
                                .modelId(selectedModel.getId())
                                .requestModel(selectedModel.getModelKey())
                                .requestType("chat")
                                .source(apiKeyId != null ? "api" : "web")
                                .promptTokens(promptTokens[0])
                                .completionTokens(completionTokens[0])
                                .totalTokens(totalTokens)
                                .duration((int) duration)
                                .status("success")
                                .routingStrategy(strategyType)
                                .isFallback(false)
                                .clientIp(clientIp)
                                .userAgent(userAgent)
                                .build());

                        // 收集监控指标（流式）
                        aiMetricsCollector.recordRequest(selectedModel.getModelKey(), userId, apiKeyId != null ? apiKeyId.toString() : null);
                        aiMetricsCollector.recordTokens(selectedModel.getModelKey(), totalTokens);
                        aiMetricsCollector.recordResponseTime(selectedModel.getModelKey(), duration);

                        // 扣减用户配额和余额（BYOK 模式下免费，不扣减）
                        if (userId != null && totalTokens > 0 && !isByok[0]) {
                            quotaService.deductTokens(userId, totalTokens);

                            // 计算费用并扣减余额
                            java.math.BigDecimal cost = billingService.calculateCost(
                                    selectedModel,
                                    promptTokens[0],
                                    completionTokens[0]
                            );

                            if (cost.compareTo(java.math.BigDecimal.ZERO) > 0) {
                                // 根据来源区分描述
                                String description = apiKeyId != null
                                        ? "API调用消费（流式） - " + selectedModel.getModelKey()
                                        : "网页调用消费（流式） - " + selectedModel.getModelKey();
                                balanceService.deductBalance(userId, cost, null, description);
                            }
                        } else if (isByok[0]) {
                            log.info("BYOK 模式（流式）：用户 {} 使用自己的密钥，不扣减余额和配额", userId);
                        }
                    }).doOnError(error -> {
                        // 流错误时记录日志
                        log.error("流式调用模型失败", error);
                        long duration = System.currentTimeMillis() - startTime;
                        requestLogService.logRequest(RequestLogDTO.builder()
                                .traceId(traceId)
                                .userId(userId)
                                .apiKeyId(apiKeyId)
                                .modelId(selectedModel.getId())
                                .requestModel(selectedModel.getModelKey())
                                .requestType("chat")
                                .source(apiKeyId != null ? "api" : "web")
                                .duration((int) duration)
                                .status("failed")
                                .errorMessage(error.getMessage())
                                .errorCode("STREAM_ERROR")
                                .routingStrategy(strategyType)
                                .isFallback(false)
                                .clientIp(clientIp)
                                .userAgent(userAgent)
                                .build());
                    });
        } catch (Exception e) {
            log.error("流式调用模型失败", e);
            return Flux.error(new BusinessException(ErrorCode.SYSTEM_ERROR, "流式调用模型失败: " + e.getMessage()));
        }
    }

//    public Flux<String> chatStream(ChatRequest chatRequest, Long userId, Long apiKeyId) {
//        String modelName = chatRequest.getModel();
//        long startTime = System.currentTimeMillis();
//
//        // Token 计数器
//        final int[] promptTokens = {0};
//        final int[] completionTokens = {0};
//
//        try {
//            // 1. 构建 Prompt
//            Prompt prompt = buildPrompt(chatRequest);
//
//            // 2. 调用流式 AI 模型
//            ChatClient chatClient = ChatClient.builder(chatModel).build();
//            Flux<org.springframework.ai.chat.model.ChatResponse> flux = chatClient
//                    .prompt(prompt)
//                    .stream()
//                    .chatResponse();
//
//            // 3. 处理流式响应
//            return flux.flatMap(response -> {
//                // 获取 Token 信息（通常只有最后一个 chunk 才有）
//                if (response.getMetadata().getUsage() != null) {
//                    Integer promptToken = response.getMetadata().getUsage().getPromptTokens();
//                    Integer completion = response.getMetadata().getUsage().getCompletionTokens();
//
//                    if (promptToken != null && promptToken > 0) {
//                        promptTokens[0] = promptToken;
//                    }
//                    if (completion != null && completion > 0) {
//                        completionTokens[0] = completion;
//                    }
//                }
//
//                // 检查是否有内容（最后一个 chunk 只有 token 信息，没有内容）
//                if (ObjectUtils.isEmpty(response.getResult()) || ObjectUtils.isEmpty(response.getResult().getOutput())) {
//                    return Flux.empty();
//                }
//
//                // 将文本中的换行符转义为 \\n，避免与 SSE 格式冲突
//                String text = response.getResult().getOutput().getText();
//                String escapedText = text.replace("\n", "\\n");
//                return Flux.just(escapedText + "\n\n");
//            }).doOnComplete(() -> {
//                // 4. 流结束时记录日志
//                long duration = System.currentTimeMillis() - startTime;
//                int totalTokens = promptTokens[0] + completionTokens[0];
//
//                requestLogService.logRequest(userId, apiKeyId, modelName,
//                        promptTokens[0], completionTokens[0], totalTokens,
//                        (int) duration, "success", null);
//            }).doOnError(error -> {
//                // 5. 流错误时记录日志
//                log.error("流式调用模型失败", error);
//                long duration = System.currentTimeMillis() - startTime;
//                requestLogService.logRequest(userId, apiKeyId, modelName, 0, 0, 0,
//                        (int) duration, "failed", error.getMessage());
//            });
//        } catch (Exception e) {
//            log.error("流式调用模型失败", e);
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流式调用模型失败: " + e.getMessage());
//        }
//    }

}
