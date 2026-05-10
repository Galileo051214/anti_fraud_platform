package com.anti.entity.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String grade;

    private String major;

    private Integer knowledgeLevel;

    private String weakPoints;

    private String interestTags;
}
