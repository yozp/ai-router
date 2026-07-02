package com.yzj.airouter.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.yzj.airouter.model.dto.log.RequestLogDTO;
import com.yzj.airouter.model.dto.log.RequestLogQueryRequest;
import com.yzj.airouter.model.entity.RequestLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    /**
     * 统计用户总请求数
     */
    Long countUserRequests(Long userId);

    /**
     * 统计用户成功请求数
     */
    Long countUserSuccessRequests(Long userId);

    /**
     * 获取用户每日统计数据
     */
    List<Map<String, Object>> getUserDailyStats(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 分页查询调用历史
     */
    Page<RequestLog> pageByQuery(RequestLogQueryRequest queryRequest);

    /**
     * 根据ID获取日志详情
     */
    RequestLog getById(Long id);
}
