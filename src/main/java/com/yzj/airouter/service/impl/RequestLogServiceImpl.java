package com.yzj.airouter.service.impl;

import cn.hutool.core.util.IdUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yzj.airouter.mapper.RequestLogMapper;
import com.yzj.airouter.model.dto.log.RequestLogDTO;
import com.yzj.airouter.model.entity.RequestLog;
import com.yzj.airouter.service.ApiKeyService;
import com.yzj.airouter.service.BillingService;
import com.yzj.airouter.service.RequestLogService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/yozp">yunikon</a>
 */
@Service
public class RequestLogServiceImpl extends ServiceImpl<RequestLogMapper, RequestLog> implements RequestLogService {


    @Resource
    private RequestLogMapper requestLogMapper;

    @Resource
    private ApiKeyService apiKeyService;

    @Resource
    private BillingService billingService;

    @Override
    @Async
    public void logRequest(RequestLogDTO logDTO) {
        if (logDTO == null) {
            return;
        }

        // 如果没有 traceId，生成一个
        if (logDTO.getTraceId() == null) {
            logDTO.setTraceId(IdUtil.simpleUUID());
        }

        // 计算费用（如果没有提供费用，则自动计算）
        BigDecimal cost = logDTO.getCost();
        if (cost == null && logDTO.getModelId() != null && "success".equals(logDTO.getStatus())) {
            int promptTokens = logDTO.getPromptTokens() != null ? logDTO.getPromptTokens() : 0;
            int completionTokens = logDTO.getCompletionTokens() != null ? logDTO.getCompletionTokens() : 0;
            cost = billingService.calculateCost(logDTO.getModelId(), promptTokens, completionTokens);
        }

        // 创建请求日志
        RequestLog log = RequestLog.builder()
                .traceId(logDTO.getTraceId())
                .userId(logDTO.getUserId())
                .apiKeyId(logDTO.getApiKeyId())
                .modelId(logDTO.getModelId())
                .requestModel(logDTO.getRequestModel())
                .modelName(logDTO.getRequestModel())  // 兼容字段
                .requestType(logDTO.getRequestType() != null ? logDTO.getRequestType() : "chat")
                .source(logDTO.getSource() != null ? logDTO.getSource() : "web")
                .promptTokens(logDTO.getPromptTokens() != null ? logDTO.getPromptTokens() : 0)
                .completionTokens(logDTO.getCompletionTokens() != null ? logDTO.getCompletionTokens() : 0)
                .totalTokens(logDTO.getTotalTokens() != null ? logDTO.getTotalTokens() : 0)
                .cost(cost != null ? cost : BigDecimal.ZERO)
                .duration(logDTO.getDuration() != null ? logDTO.getDuration() : 0)
                .status(logDTO.getStatus())
                .errorMessage(logDTO.getErrorMessage())
                .errorCode(logDTO.getErrorCode())
                .routingStrategy(logDTO.getRoutingStrategy())
                .isFallback(logDTO.getIsFallback() != null && logDTO.getIsFallback() ? 1 : 0)
                .clientIp(logDTO.getClientIp())
                .userAgent(logDTO.getUserAgent())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 保存日志
        requestLogMapper.insert(log);

        // 更新 API Key 的使用统计（仅成功的请求且有 apiKeyId）
        if ("success".equals(logDTO.getStatus()) &&
                logDTO.getApiKeyId() != null &&
                logDTO.getTotalTokens() != null &&
                logDTO.getTotalTokens() > 0) {
            apiKeyService.updateUsageStats(logDTO.getApiKeyId(), logDTO.getTotalTokens());
        }
    }

    @Override
    public List<RequestLog> listUserLogs(Long userId, Integer limit) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .orderBy("createTime", false)
                .limit(limit != null ? limit : 100);

        return requestLogMapper.selectListByQuery(queryWrapper);
    }

    @Override
    public Long countUserTokens(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("SUM(totalTokens)")
                .eq("userId", userId)
                .eq("status", "success");

        Long total = requestLogMapper.selectObjectByQueryAs(queryWrapper, Long.class);
        return total != null ? total : 0L;
    }
}
