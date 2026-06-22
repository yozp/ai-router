package com.yzj.airouter.service;

import com.mybatisflex.core.service.IService;
import com.yzj.airouter.model.dto.log.RequestLogDTO;
import com.yzj.airouter.model.entity.RequestLog;
import java.util.List;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/yozp">yunikon</a>
 */
public interface RequestLogService extends IService<RequestLog> {

    /**
     * 记录请求日志
     */
    void logRequest(RequestLogDTO logDTO);

    /**
     * 查询用户的请求日志
     */
    List<RequestLog> listUserLogs(Long userId, Integer limit);

    /**
     * 统计用户的 Token 消耗
     */
    Long countUserTokens(Long userId);
}
