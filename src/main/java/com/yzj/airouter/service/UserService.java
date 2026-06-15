package com.yzj.airouter.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yzj.airouter.model.dto.user.UserQueryRequest;
import com.yzj.airouter.model.entity.User;
import com.yzj.airouter.model.vo.LoginUserVO;
import com.yzj.airouter.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/yozp">yunikon</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取加密密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏的登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户注销
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取用户列表（仅管理员）
     */
    List<UserVO> getUserVOList(List<User> userList);

    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

}
