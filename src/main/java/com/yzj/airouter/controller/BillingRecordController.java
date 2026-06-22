package com.yzj.airouter.controller;

import com.mybatisflex.core.paginate.Page;
import com.yzj.airouter.model.entity.BillingRecord;
import com.yzj.airouter.service.BillingRecordService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 *  控制层。
 *
 * @author <a href="https://github.com/yozp">yunikon</a>
 */
@RestController
@RequestMapping("/billingRecord")
public class BillingRecordController {

    @Resource
    private BillingRecordService billingRecordService;

    /**
     * 保存。
     *
     * @param billingRecord 
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody BillingRecord billingRecord) {
        return billingRecordService.save(billingRecord);
    }

    /**
     * 根据主键删除。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return billingRecordService.removeById(id);
    }

    /**
     * 根据主键更新。
     *
     * @param billingRecord 
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody BillingRecord billingRecord) {
        return billingRecordService.updateById(billingRecord);
    }

    /**
     * 查询所有。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<BillingRecord> list() {
        return billingRecordService.list();
    }

    /**
     * 根据主键获取。
     *
     * @param id 主键
     * @return 详情
     */
    @GetMapping("getInfo/{id}")
    public BillingRecord getInfo(@PathVariable Long id) {
        return billingRecordService.getById(id);
    }

    /**
     * 分页查询。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<BillingRecord> page(Page<BillingRecord> page) {
        return billingRecordService.page(page);
    }

}
