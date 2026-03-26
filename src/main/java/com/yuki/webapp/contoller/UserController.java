package com.yuki.webapp.contoller;

import com.yuki.webapp.pojo.*;
import com.yuki.webapp.service.EmailService;
import com.yuki.webapp.service.UserService;
import com.yuki.webapp.service.VerificationCodeService;
import com.yuki.webapp.utils.JwtUtil;
import com.yuki.webapp.utils.Md5Util;
import com.yuki.webapp.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Validated
public class UserController extends HttpServlet {
    @Autowired
    private UserService userService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterUser user) {
        String userName= user.getUserName();
        String userPassword= user.getUserPassword();
        String userEmail=user.getUserEmail();
        String code=user.getCode();
        //查询用户
        if (verificationCodeService.verifyCode(userEmail, code)) {
            userService.register(userName, userPassword,userEmail);
            return Result.success();
        } else {
            return Result.error("验证码错误");
        }
    }

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody User user) {
        //根据用户名查询用户
        String userEmail= user.getUserEmail();
        String userPassword=user.getUserPassword();
        User loginUser = userService.findByUserEmail(userEmail);
        //判断该用户是否存在
        if (loginUser == null) {
            return Result.error("邮箱错误");
        }

        //判断密码是否正确  loginUser对象中的password是密文
        if (Md5Util.getMD5String(userPassword).equals(loginUser.getUserPassword())) {
            //登录成功
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", loginUser.getUserId());
            claims.put("userEmail", loginUser.getUserEmail());
            String token = JwtUtil.genToken(claims);
            //把token存储到redis中
//            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
//            operations.set(token,token,1, TimeUnit.HOURS);
            return Result.success(token);
        }
        return Result.error("密码错误");
    }

    @GetMapping("/userInfo")
    //获取用户详细信息
    public Result<User> userInfo() {
        //根据邮箱查询用户
        Map<String, Object> map = ThreadLocalUtil.get();
        String userEmail = (String) map.get("userEmail");
        User user = userService.findByUserEmail(userEmail);
        user.setUserPassword(null);
        return Result.success(user);
    }

    @PutMapping("/update")
    public Result update(@RequestBody @Validated User user) {
        userService.update(user);
        return Result.success();
    }

    // 根据userId查询用户信息
    @GetMapping("userInfoById")
    public Result<UserDTO> userInfoById(@RequestParam("userId") Integer userId) {
        UserDTO userInfo = userService.getUserInfoById(userId);
        userInfo.setUserPassword(null);
        return Result.success(userInfo);
    }

    // 修改密码
    @PutMapping("/changePassword")
    public Result updatePassword(@RequestBody @Validated PasswordDTO passwordDTO) {
        // 验证两次输入的新密码是否一致
        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            return Result.error("两次输入的新密码不一致");
        }
        return userService.updatePassword(passwordDTO);
    }
}
