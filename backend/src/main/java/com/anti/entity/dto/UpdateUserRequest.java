package com.anti.entity.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private Long id;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private String grade;
    private String major;
}
