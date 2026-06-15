package com.yzj.airouter.controller;

import com.mybatisflex.core.paginate.Page;
import com.yzj.airouter.model.entity.RequestLog;
import com.yzj.airouter.service.RequestLogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 *  控制层。
 *
 * @author <a href="https://github.com/yozp">yunikon</a>
 */
@RestController
@RequestMapping("/requestLog")
public class RequestLogController {

    @Resource
    private RequestLogService requestLogService;

    /**
     * 保存。
     *
     * @param requestLog 
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody RequestLog requestLog) {
        return requestLogService.save(requestLog);
    }

    /**
     * 根据主键删除。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return requestLogService.removeById(id);
    }

    /**
     * 根据主键更新。
     *
     * @param requestLog 
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody RequestLog requestLog) {
        return requestLogService.updateById(requestLog);
    }

    /**
     * 查询所有。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<RequestLog> list() {
        return requestLogService.list();
    }

    /**
     * 根据主键获取。
     *
     * @param id 主键
     * @return 详情
     */
    @GetMapping("getInfo/{id}")
    public RequestLog getInfo(@PathVariable Long id) {
        return requestLogService.getById(id);
    }

    /**
     * 分页查询。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<RequestLog> page(Page<RequestLog> page) {
        return requestLogService.page(page);
    }

}
