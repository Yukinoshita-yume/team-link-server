package com.yuki.webapp.service;

//发送验证码
public interface EmailService {
    void sendVerificationCode(String to, String code);
}
