package com.yuki.webapp.pojo;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordDTO {
    private String userEmail;

    @Pattern(regexp = "^\\S{5,16}$", message = "密码必须是5-16位的非空字符")
    private String newPassword;

    @Pattern(regexp = "^\\S{5,16}$", message = "确认密码必须是5-16位的非空字符")
    private String confirmPassword;

    private String code; // 验证码
}
