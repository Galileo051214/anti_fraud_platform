package com.anti.entity.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private String role;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
}
