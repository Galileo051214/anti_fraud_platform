package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.User;
import com.anti.entity.dto.*;
import com.anti.entity.vo.LoginVO;
import com.anti.entity.vo.UserVO;
import com.anti.mapper.UserMapper;
import com.anti.security.JwtUtils;
import com.anti.service.AchievementService;
import com.anti.service.ProfileService;
import com.anti.service.ScoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements com.anti.service.UserService {

    private final UserMapper userMapper;
    private final ScoreService scoreService;
    private final AchievementService achievementService;
    private final ProfileService profileService;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    @Override
    public LoginVO login(LoginRequest request) {
        String username = StringUtils.hasText(request.getUsername()) ? request.getUsername().trim() : "";
        String password = request.getPassword();

        if (!StringUtils.hasText(username)) {
            throw new BusinessException("用户名不能为空");
        }

        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getId());

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        try {
            achievementService.checkAndUnlockAchievements(user.getId(), "login_count", 1);
            achievementService.refreshContinuousLearningStreak(user.getId());
        } catch (Exception e) {
            log.warn("登录后成就校验失败 userId={}", user.getId(), e);
        }

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRole(user.getRole());
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setAvatar(user.getAvatar());

        log.info("用户登录成功: {}", username);
        return loginVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(RegisterRequest request) {
        String username = request.getUsername();

        User existUser = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );

        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        if (StringUtils.hasText(request.getStudentNo())) {
            User existStudent = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentNo, request.getStudentNo())
            );
            if (existStudent != null) {
                throw new BusinessException("学号已被注册");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : username);
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStudentNo(request.getStudentNo());
        user.setGrade(request.getGrade());
        user.setMajor(request.getMajor());
        user.setRole("student");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);

        scoreService.initScore(user.getId());
        profileService.initProfile(user.getId(), user.getGrade(), user.getMajor());

        log.info("用户注册成功: {}", username);
        return user;
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UpdateUserRequest request) {
        User user = userMapper.selectById(request.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }
        if (StringUtils.hasText(request.getGrade())) {
            user.setGrade(request.getGrade());
        }
        if (StringUtils.hasText(request.getMajor())) {
            user.setMajor(request.getMajor());
        }

        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户信息更新: userId={}", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户密码修改: userId={}", userId);
    }

    @Override
    public IPage<UserVO> getUserList(Page<User> page, String keyword, String role, Integer status) {
        IPage<User> userPage = userMapper.selectUserPage(page, keyword, role, status);
        IPage<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());

        voPage.setRecords(userPage.getRecords().stream().map(this::convertToUserVO).toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(1);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("启用用户: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(0);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("禁用用户: userId={}", userId);
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToUserVO(user);
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (jwtUtils.validateToken(token)) {
            Long expiration = jwtUtils.getExpirationFromToken(token);
            if (expiration > 0) {
                redisTemplate.opsForValue().set(
                    TOKEN_BLACKLIST_PREFIX + token,
                    "blacklisted",
                    expiration,
                    TimeUnit.MILLISECONDS
                );
            }
        }
        log.info("用户退出登录");
    }

    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setStudentNo(user.getStudentNo());
        return vo;
    }
}
