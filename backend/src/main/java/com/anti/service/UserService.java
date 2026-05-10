package com.anti.service;

import com.anti.entity.User;
import com.anti.entity.dto.*;
import com.anti.entity.vo.LoginVO;
import com.anti.entity.vo.UserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface UserService {

    LoginVO login(LoginRequest request);

    User register(RegisterRequest request);

    UserVO getUserInfo(Long userId);

    void updateUser(UpdateUserRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    IPage<UserVO> getUserList(Page<User> page, String keyword, String role, Integer status);

    void enableUser(Long userId);

    void disableUser(Long userId);

    UserVO getUserById(Long userId);

    void logout(String token);
}
