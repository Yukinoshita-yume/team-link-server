package com.yuki.webapp.service.impl;


import com.yuki.webapp.mapper.UserMapper;
import com.yuki.webapp.pojo.PasswordDTO;
import com.yuki.webapp.pojo.Result;
import com.yuki.webapp.pojo.User;
import com.yuki.webapp.pojo.UserDTO;
import com.yuki.webapp.service.UserService;
import com.yuki.webapp.service.UserProfileMySqlService;
import com.yuki.webapp.service.VerificationCodeService;
import com.yuki.webapp.utils.Md5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired(required = false)
    private UserProfileMySqlService userProfileMySqlService;

    @Override
    public User findByUserEmail(String userEmail) {
        User u = userMapper.findByUserEmail(userEmail);
        return u;
    }

    @Override
    public void register(String userName, String userPassword, String userEmail) {
        //加密
        String md5String = Md5Util.getMD5String(userPassword);
        //添加
        userMapper.add(userName, md5String, userEmail);
    }

    @Override
    public void update(User user) {
        String previousIntro = null;
        if (user.getUserId() != null) {
            UserDTO existing = getUserInfoById(user.getUserId());
            if (existing != null) {
                previousIntro = existing.getUserInformation();
            }
        }

        user.setUserUpdateTime(LocalDateTime.now());
        userMapper.update(user);

        // 简介字段有提交且内容相对数据库有变化时，重新生成画像并同步 user_skill_tag（一对多）
        if (userProfileMySqlService == null || user.getUserId() == null || user.getUserInformation() == null) {
            return;
        }
        if (!introChanged(previousIntro, user.getUserInformation())) {
            return;
        }
        try {
            userProfileMySqlService.generateAndSaveFromIntro(user.getUserId());
        } catch (Exception ignored) {
        }
    }

    private static boolean introChanged(String previous, String submitted) {
        String a = previous == null ? "" : previous.trim();
        String b = submitted == null ? "" : submitted.trim();
        return !a.equals(b);
    }

    @Override
    public Boolean checkUserEmail(String userEmail) {
        return userMapper.checkUserEmail(userEmail);
    }

    @Override
    public UserDTO getUserInfoById(Integer userId) {
        return userMapper.getUserInfoById(userId);
    }

    @Override
    public Result updatePassword(PasswordDTO passwordDTO) {
        String userEmail = passwordDTO.getUserEmail();
        String newPassword = passwordDTO.getNewPassword();
        String code = passwordDTO.getCode();

        // 1. 验证验证码是否正确
        if (!verificationCodeService.verifyCode(userEmail, code)) {
            return Result.error("验证码错误");
        }

        // 2. 验证用户是否存在
        String currentMd5Password = userMapper.selectPasswordByEmail(userEmail);
        if (currentMd5Password == null) {
            return Result.error("用户不存在");
        }

        // 3. 检查新密码是否与旧密码相同
        String inputOldMd5 = Md5Util.getMD5String(newPassword);
        if (inputOldMd5.equals(currentMd5Password)) {
            return Result.error("新密码不能与旧密码一致");
        }

        // 4. 对新密码进行MD5加密
        String newMd5Password = Md5Util.getMD5String(newPassword);

        // 5. 更新数据库中的MD5密码
        int result = userMapper.updatePasswordByEmail(userEmail, newMd5Password);

        return result > 0 ? Result.success() : Result.error("密码修改失败");
    }
}
