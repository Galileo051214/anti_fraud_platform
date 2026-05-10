package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.dto.UpdateProfileRequest;
import com.anti.entity.vo.ProfileVO;
import com.anti.security.LoginUser;
import com.anti.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "用户画像")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/info")
    @Operation(summary = "获取用户画像")
    public Result<ProfileVO> getProfileInfo(@AuthenticationPrincipal LoginUser loginUser) {
        var profile = profileService.getProfileByUserId(loginUser.getUserId());
        var vo = new ProfileVO();
        vo.setId(profile.getId());
        vo.setUserId(profile.getUserId());
        vo.setGrade(profile.getGrade());
        vo.setMajor(profile.getMajor());
        vo.setKnowledgeLevel(profile.getKnowledgeLevel());
        vo.setWeakPoints(profileService.getWeakPoints(loginUser.getUserId()));
        vo.setInterestTags(profileService.getInterestTags(loginUser.getUserId()));
        vo.setLifecycleStage(profile.getLifecycleStage());
        vo.setBrowseCount(profile.getBrowseCount());
        vo.setRegisterDays(profile.getRegisterDays());
        vo.setUpdateTime(profile.getUpdateTime());
        return Result.success(vo);
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户画像")
    public Result<Void> updateProfile(@RequestBody UpdateProfileRequest request, @AuthenticationPrincipal LoginUser loginUser) {
        profileService.updateProfile(loginUser.getUserId(), request);
        return Result.success();
    }

    @PostMapping("/browse")
    @Operation(summary = "增加浏览次数")
    public Result<Void> incrementBrowse(@AuthenticationPrincipal LoginUser loginUser) {
        profileService.incrementBrowseCount(loginUser.getUserId());
        return Result.success();
    }

    @GetMapping("/weak-points")
    @Operation(summary = "获取弱点标签")
    public Result<List<String>> getWeakPoints(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(profileService.getWeakPoints(loginUser.getUserId()));
    }

    @GetMapping("/interest-tags")
    @Operation(summary = "获取兴趣标签")
    public Result<List<String>> getInterestTags(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(profileService.getInterestTags(loginUser.getUserId()));
    }

    @PostMapping("/weak-point/{tag}")
    @Operation(summary = "添加弱点标签")
    public Result<Void> addWeakPoint(@PathVariable String tag, @AuthenticationPrincipal LoginUser loginUser) {
        profileService.addWeakPoint(loginUser.getUserId(), tag);
        return Result.success();
    }

    @PostMapping("/interest-tag/{tag}")
    @Operation(summary = "添加兴趣标签")
    public Result<Void> addInterestTag(@PathVariable String tag, @AuthenticationPrincipal LoginUser loginUser) {
        profileService.addInterestTag(loginUser.getUserId(), tag);
        return Result.success();
    }

    @GetMapping("/lifecycle")
    @Operation(summary = "获取生命周期阶段")
    public Result<String> getLifecycleStage(@AuthenticationPrincipal LoginUser loginUser) {
        String stage = profileService.determineLifecycleStage(loginUser.getUserId());
        profileService.updateLifecycleStage(loginUser.getUserId());
        return Result.success(stage);
    }
}
