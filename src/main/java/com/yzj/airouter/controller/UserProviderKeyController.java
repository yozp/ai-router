package com.yzj.airouter.controller;

import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.yzj.airouter.model.entity.UserProviderKey;
import com.yzj.airouter.service.UserProviderKeyService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 *  控制层。
 *
 * @author <a href="https://github.com/yozp">yunikon</a>
 */
@RestController
@RequestMapping("/userProviderKey")
public class UserProviderKeyController {

    @Resource
    private UserProviderKeyService userProviderKeyService;

    /**
     * 保存。
     *
     * @param userProviderKey 
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody UserProviderKey userProviderKey) {
        return userProviderKeyService.save(userProviderKey);
    }

    /**
     * 根据主键删除。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return userProviderKeyService.removeById(id);
    }

    /**
     * 根据主键更新。
     *
     * @param userProviderKey 
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody UserProviderKey userProviderKey) {
        return userProviderKeyService.updateById(userProviderKey);
    }

    /**
     * 查询所有。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<UserProviderKey> list() {
        return userProviderKeyService.list();
    }

    /**
     * 根据主键获取。
     *
     * @param id 主键
     * @return 详情
     */
    @GetMapping("getInfo/{id}")
    public UserProviderKey getInfo(@PathVariable Long id) {
        return userProviderKeyService.getById(id);
    }

    /**
     * 分页查询。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<UserProviderKey> page(Page<UserProviderKey> page) {
        return userProviderKeyService.page(page);
    }

}
