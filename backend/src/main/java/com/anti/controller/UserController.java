package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.User;
import com.anti.entity.dto.*;
import com.anti.entity.vo.LoginVO;
import com.anti.entity.vo.UserVO;
import com.anti.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理", description = "用户注册、登录、信息管理接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册", description = "注册新用户，支持学生和管理员注册")
    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return Result.success("注册成功", user);
    }

    @Operation(summary = "用户登录", description = "用户名密码登录，返回JWT Token")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequest request) {
        LoginVO loginVO = userService.login(request);
        return Result.success("登录成功", loginVO);
    }

    @Operation(summary = "获取当前用户信息", description = "获取已登录用户的详细信息")
    @GetMapping("/info")
    public Result<UserVO> getUserInfo(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UserVO userInfo = userService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    @Operation(summary = "更新用户信息", description = "更新当前用户的个人信息")
    @PutMapping("/update")
    public Result<Void> updateUser(@RequestBody UpdateUserRequest request, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        request.setId(userId);
        userService.updateUser(request);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "修改密码", description = "修改当前用户的登录密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request,
                                       Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        userService.changePassword(userId, request);
        return Result.success("密码修改成功", null);
    }

    @Operation(summary = "退出登录", description = "将当前Token加入黑名单")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        userService.logout(token);
        return Result.success("退出成功", null);
    }

    @Operation(summary = "获取用户列表", description = "管理员获取所有用户列表，支持分页和筛选")
    @GetMapping("/list")
    public Result<IPage<UserVO>> getUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "角色筛选") @RequestParam(required = false) String role,
            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status) {
        Page<User> page = new Page<>(pageNum, pageSize);
        IPage<UserVO> result = userService.getUserList(page, keyword, role, status);
        return Result.<IPage<UserVO>>success(result);
    }

    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息(管理员)")
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        UserVO user = userService.getUserById(id);
        return Result.success(user);
    }

    @Operation(summary = "管理员更新用户信息", description = "管理员更新指定用户的个人信息")
    @PutMapping("/{id}")
    public Result<Void> adminUpdateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {
        request.setId(id);
        userService.updateUser(request);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "启用用户", description = "管理员启用被禁用的用户账号")
    @PutMapping("/{id}/enable")
    public Result<Void> enableUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        userService.enableUser(id);
        return Result.success("用户已启用", null);
    }

    @Operation(summary = "禁用用户", description = "管理员禁用用户账号，被禁用的用户无法登录")
    @PutMapping("/{id}/disable")
    public Result<Void> disableUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        userService.disableUser(id);
        return Result.success("用户已禁用", null);
    }
}
