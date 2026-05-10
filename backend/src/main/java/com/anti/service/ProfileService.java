package com.anti.service;

import com.anti.entity.UserProfile;
import com.anti.entity.dto.UpdateProfileRequest;

import java.util.List;

public interface ProfileService {

    UserProfile getProfileByUserId(Long userId);

    void updateProfile(Long userId, UpdateProfileRequest request);

    void incrementBrowseCount(Long userId);

    void updateKnowledgeLevel(Long userId, Integer level);

    void addWeakPoint(Long userId, String weakPoint);

    void addInterestTag(Long userId, String interestTag);

    void updateLifecycleStage(Long userId);

    List<String> getWeakPoints(Long userId);

    List<String> getInterestTags(Long userId);

    String determineLifecycleStage(Long userId);

    void initProfile(Long userId, String grade, String major);
}
