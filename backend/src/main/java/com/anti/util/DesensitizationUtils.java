package com.anti.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

public class DesensitizationUtils {

    public static String desensitizePhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return "";
        }
        // 隐藏中间4位
        if (phone.length() >= 7) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return phone;
    }

    public static String desensitizeEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return "";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (username.length() <= 2) {
            return "**" + domain;
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + domain;
    }

    public static String desensitizeStudentNo(String studentNo) {
        if (StrUtil.isBlank(studentNo)) {
            return "";
        }
        // 学号脱敏：保留前3位和后3位
        if (studentNo.length() > 6) {
            return studentNo.substring(0, 3) + "****" + studentNo.substring(studentNo.length() - 3);
        }
        return studentNo;
    }

    public static String desensitizeIdCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return "";
        }
        if (idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }
}
