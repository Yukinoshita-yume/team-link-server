package com.yuki.webapp.pojo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUser {
    @Size(max = 18, message = "用户名不能超过18字节")//正则表达式规定账号和密码长度
    private String userName;
    @Email
    private String userEmail;
    //    @JsonIgnore 注解会导致接受的json参数的userPassword被置空
    @Pattern(regexp = "^\\S{5,16}$", message = "密码必须是5到16位的非空字符")
    private String userPassword;
    private String code;
}
