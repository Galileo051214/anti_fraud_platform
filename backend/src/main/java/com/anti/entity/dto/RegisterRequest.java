package com.anti.entity.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String email;
    private String studentNo;
    private String grade;
    private String major;
}
