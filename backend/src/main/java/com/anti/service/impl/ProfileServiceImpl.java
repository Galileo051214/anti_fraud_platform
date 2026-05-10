package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.User;
import com.anti.entity.UserProfile;
import com.anti.mapper.CaseBrowseLogMapper;
import com.anti.mapper.NewsBrowseLogMapper;
import com.anti.mapper.UserMapper;
import com.anti.mapper.UserProfileMapper;
import com.anti.service.ProfileService;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileMapper profileMapper;
    private final UserMapper userMapper;
    private final CaseBrowseLogMapper caseBrowseLogMapper;
    private final NewsBrowseLogMapper newsBrowseLogMapper;

    private static final int NEWBIE_BROWSE_THRESHOLD = 5;
    private static final int GROWING_BROWSE_THRESHOLD = 20;

    @Override
    public UserProfile getProfileByUserId(Long userId) {
        UserProfile profile = profileMapper.selectByUserId(userId);
        if (profile == null) {
            throw new BusinessException(404, "用户画像不存在");
        }
        return profile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, com.anti.entity.dto.UpdateProfileRequest request) {
        UserProfile profile = profileMapper.selectByUserId(userId);
        if (profile == null) {
            throw new BusinessException(404, "用户画像不存在");
        }
        if (request.getGrade() != null) {
            profile.setGrade(request.getGrade());
        }
        if (request.getMajor() != null) {
            profile.setMajor(request.getMajor());
        }
        if (request.getKnowledgeLevel() != null) {
            profile.setKnowledgeLevel(request.getKnowledgeLevel());
        }
        profile.setUpdateTime(LocalDateTime.now());
        profileMapper.updateById(profile);
        log.info("更新用户 {} 画像", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementBrowseCount(Long userId) {
        UserProfile profile = profileMapper.selectByUserId(userId);
        if (profile == null) {
            return;
        }
        profile.setBrowseCount(profile.getBrowseCount() + 1);
        profile.setUpdateTime(LocalDateTime.now());
        profileMapper.updateById(profile);
        updateLifecycleStage(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateKnowledgeLevel(Long userId, Integer level) {
        UserProfile profile = profileMapper.selectByUserId(userId);
        if (profile == null) {
            return;
        }
        int newLevel = Math.max(0, Math.min(100, level));
        profile.setKnowledgeLevel(newLevel);
        profile.setUpdateTime(LocalDateTime.now());
        profileMapper.updateById(profile);
        log.info("更新用户 {} 知识水平为 {}", userId, newLevel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addWeakPoint(Long userId, String weakPoint) {
        UserProfile profile = profileMapper.selectByUserId(userId);
        if (profile == null) {
            return;
        }
        List<String> weakPoints = getWeakPoints(userId);
        if (!weakPoints.contains(weakPoint)) {
            weakPoints.add(weakPoint);
            profile.setWeakPoints(JSONUtil.toJsonStr(weakPoints));
            profile.setUpdateTime(LocalDateTime.now());
            profileMapper.updateById(profile);
            log.info("用户 {} 添加弱点标签: {}", userId, weakPoint);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addInterestTag(Long userId, String interestTag) {
        UserProfile profile = profileMapper.selectByUserId(userId);
        if (profile == null) {
            return;
        }
        List<String> interestTags = getInterestTags(userId);
        if (!interestTags.contains(interestTag)) {
            interestTags.add(interestTag);
            profile.setInterestTags(JSONUtil.toJsonStr(interestTags));
            profile.setUpdateTime(LocalDateTime.now());
            profileMapper.updateById(profile);
            log.info("用户 {} 添加兴趣标签: {}", userId, interestTag);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLifecycleStage(Long userId) {
        UserProfile profile = profileMapper.selectByUserId(userId);
        if (profile == null) {
            return;
        }
        String newStage = determineLifecycleStage(userId);
        if (!newStage.equals(profile.getLifecycleStage())) {
            profile.setLifecycleStage(newStage);
            profile.setUpdateTime(LocalDateTime.now());
            profileMapper.updateById(profile);
            log.info("用户 {} 生命周期阶段从 {} 更新为 {}", userId, profile.getLifecycleStage(), newStage);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getWeakPoints(Long userId) {
        UserProfile profile = profileMapper.selectByUserId(userId);
        if (profile == null || profile.getWeakPoints() == null) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(profile.getWeakPoints(), String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getInterestTags(Long userId) {
        UserProfile profile = profileMapper.selectByUserId(userId);
        if (profile == null || profile.getInterestTags() == null) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(profile.getInterestTags(), String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public String determineLifecycleStage(Long userId) {
        log.info("=== 开始计算生命周期阶段: userId={} ===", userId);
        UserProfile profile = profileMapper.selectByUserId(userId);
        if (profile == null) {
            log.warn("=== 用户画像不存在，返回新手期 ===");
            return "newbie";
        }
        User user = userMapper.selectById(userId);
        if (user == null || user.getCreateTime() == null) {
            log.warn("=== 用户不存在或注册时间为空，返回新手期 ===");
            return "newbie";
        }
        long registerDays = ChronoUnit.DAYS.between(user.getCreateTime(), LocalDateTime.now());
        
        int caseBrowseCount = caseBrowseLogMapper.countDistinctCasesByUserId(userId);
        int newsBrowseCount = newsBrowseLogMapper.countDistinctNewsByUserId(userId);
        int totalBrowseCount = caseBrowseCount + newsBrowseCount;
        
        log.info("=== 浏览统计: registerDays={}, caseBrowseCount={}, newsBrowseCount={}, totalBrowseCount={} ===", 
                registerDays, caseBrowseCount, newsBrowseCount, totalBrowseCount);
        
        profile.setRegisterDays((int) registerDays);
        profile.setBrowseCount(totalBrowseCount);
        profileMapper.updateById(profile);
        
        String stage;
        if (registerDays < 7 || totalBrowseCount < NEWBIE_BROWSE_THRESHOLD) {
            stage = "newbie";
        } else if (totalBrowseCount < GROWING_BROWSE_THRESHOLD) {
            stage = "growing";
        } else {
            stage = "mature";
        }
        log.info("=== 计算完成: lifecycleStage={} ===", stage);
        return stage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initProfile(Long userId, String grade, String major) {
        profileMapper.initProfile(userId);
        UserProfile profile = profileMapper.selectByUserId(userId);
        if (profile != null && grade != null) {
            profile.setGrade(grade);
            profile.setMajor(major);
            profile.setUpdateTime(LocalDateTime.now());
            profileMapper.updateById(profile);
        }
        log.info("初始化用户 {} 画像，年级: {}, 专业: {}", userId, grade, major);
    }
}
