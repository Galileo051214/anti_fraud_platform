package com.anti.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String studentNo;
    private String role;
    private String grade;
    private String major;
    private Integer status;
    private LocalDateTime createTime;
}
