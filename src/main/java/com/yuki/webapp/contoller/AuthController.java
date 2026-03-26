package com.yuki.webapp.contoller;

import com.yuki.webapp.pojo.Result;
import com.yuki.webapp.service.EmailService;
import com.yuki.webapp.service.UserService;
import com.yuki.webapp.service.VerificationCodeService;
import com.yuki.webapp.pojo.Result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {
    @Autowired
    private EmailService emailService;
    @Autowired
    private VerificationCodeService verificationCodeService;
    @Autowired
    private UserService userService;

    //发送验证码，保存在哈希表里
    @PostMapping("/send-code")
    public Result sendVerificationCode(@RequestBody Map<String,String> request) {
        String userEmail=request.get("userEmail");
        if(userService.checkUserEmail(userEmail)){
            return Result.error("邮箱已被占用");
        }
        else {
            String code = verificationCodeService.generateVerificationCode(userEmail);
            emailService.sendVerificationCode(userEmail, code);
            return Result.success();
        }
    }

    // 发送重置密码验证码
    @PostMapping("/sendResetCode")
    public Result sendResetCode(@RequestBody Map<String,String> request) {
        String userEmail = request.get("userEmail");
        // 检查邮箱是否已注册（与注册时的逻辑相反）
        if (!userService.checkUserEmail(userEmail)) {
            return Result.error("邮箱未注册");
        }
        String code = verificationCodeService.generateVerificationCode(userEmail);
        emailService.sendVerificationCode(userEmail, code);
        return Result.success();
    }
}
