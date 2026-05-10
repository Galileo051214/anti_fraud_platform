package com.anti.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProfileVO implements Serializable {

    private Long id;

    private Long userId;

    private String grade;

    private String major;

    private Integer knowledgeLevel;

    private List<String> weakPoints;

    private List<String> interestTags;

    private String lifecycleStage;

    private Integer browseCount;

    private Integer registerDays;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
