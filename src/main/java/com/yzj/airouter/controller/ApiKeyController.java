package com.yzj.airouter.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.yzj.airouter.annotation.AuthCheck;
import com.yzj.airouter.common.BaseResponse;
import com.yzj.airouter.common.DeleteRequest;
import com.yzj.airouter.common.ResultUtils;
import com.yzj.airouter.constant.UserConstant;
import com.yzj.airouter.exception.BusinessException;
import com.yzj.airouter.exception.ErrorCode;
import com.yzj.airouter.model.dto.apikey.ApiKeyCreateRequest;
import com.yzj.airouter.model.entity.ApiKey;
import com.yzj.airouter.model.entity.User;
import com.yzj.airouter.model.vo.ApiKeyVO;
import com.yzj.airouter.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.yzj.airouter.service.ApiKeyService;

import java.util.List;
import java.util.stream.Collectors;

/**
 *  控制层。
 *
 * @author <a href="https://github.com/yozp">yunikon</a>
 */
@RestController
@RequestMapping("/apiKey")
public class ApiKeyController {

    @Resource
    private ApiKeyService apiKeyService;

    @Resource
    private UserService userService;

    /**
     * 创建 API Key
     */
    @PostMapping("/create")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "创建 API Key")
    public BaseResponse<ApiKeyVO> createApiKey(@RequestBody ApiKeyCreateRequest request,
                                               HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        ApiKey apiKey = apiKeyService.createApiKey(request.getKeyName(), loginUser);

        // 转换为 VO（完整显示 Key 值）
        ApiKeyVO apiKeyVO = BeanUtil.copyProperties(apiKey, ApiKeyVO.class);

        return ResultUtils.success(apiKeyVO);
    }

    /**
     * 获取我的 API Key 列表（分页）
     */
    @GetMapping("/list/my")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "获取我的 API Key 列表")
    public BaseResponse<Page<ApiKeyVO>> listMyApiKeys(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        // 分页查询
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", loginUser.getId())
                .eq("isDelete", 0)
                .orderBy("createTime", false);

        Page<ApiKey> apiKeyPage = apiKeyService.page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为 VO（列表中部分隐藏 Key 值）
        Page<ApiKeyVO> apiKeyVOPage = new Page<>(pageNum, pageSize, apiKeyPage.getTotalRow());
        List<ApiKeyVO> apiKeyVOList = apiKeyPage.getRecords().stream()
                .map(apiKey -> {
                    ApiKeyVO vo = BeanUtil.copyProperties(apiKey, ApiKeyVO.class);
                    // 隐藏部分 Key 值（只显示前8位和后4位）
                    if (vo.getKeyValue() != null && vo.getKeyValue().length() > 12) {
                        String key = vo.getKeyValue();
                        vo.setKeyValue(key.substring(0, 8) + "****" + key.substring(key.length() - 4));
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        apiKeyVOPage.setRecords(apiKeyVOList);
        return ResultUtils.success(apiKeyVOPage);
    }

    /**
     * 撤销 API Key
     */
    @PostMapping("/revoke")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "撤销 API Key")
    public BaseResponse<Boolean> revokeApiKey(@RequestBody DeleteRequest deleteRequest,
                                              HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        boolean result = apiKeyService.revokeApiKey(deleteRequest.getId(), loginUser.getId());

        return ResultUtils.success(result);
    }

    //-------------------------------------------------------------------------------------------------------------------------

    /**
     * 保存。
     *
     * @param apiKey 
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody ApiKey apiKey) {
        return apiKeyService.save(apiKey);
    }

    /**
     * 根据主键删除。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return apiKeyService.removeById(id);
    }

    /**
     * 根据主键更新。
     *
     * @param apiKey 
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody ApiKey apiKey) {
        return apiKeyService.updateById(apiKey);
    }

    /**
     * 查询所有。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<ApiKey> list() {
        return apiKeyService.list();
    }

    /**
     * 根据主键获取。
     *
     * @param id 主键
     * @return 详情
     */
    @GetMapping("getInfo/{id}")
    public ApiKey getInfo(@PathVariable Long id) {
        return apiKeyService.getById(id);
    }

    /**
     * 分页查询。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<ApiKey> page(Page<ApiKey> page) {
        return apiKeyService.page(page);
    }

}
