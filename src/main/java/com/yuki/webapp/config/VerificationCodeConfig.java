package com.yuki.webapp.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VerificationCodeConfig {
    @Value("${my.verification-code.code-expire-time}")
    public long CODE_EXPIRE_TIME; // 5分钟
}
