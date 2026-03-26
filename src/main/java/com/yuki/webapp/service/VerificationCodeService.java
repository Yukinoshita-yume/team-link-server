package com.yuki.webapp.service;

public interface VerificationCodeService {
    String generateVerificationCode(String email);
    boolean verifyCode(String email, String code);
}
