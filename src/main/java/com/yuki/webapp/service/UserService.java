package com.yuki.webapp.service;

import com.yuki.webapp.pojo.PasswordDTO;
import com.yuki.webapp.pojo.Result;
import com.yuki.webapp.pojo.User;
import com.yuki.webapp.pojo.UserDTO;

public interface UserService {
    //根据邮箱查询用户
    User findByUserEmail(String userEmail);

    //注册
    void register(String userName, String userPassword,String userEmail);

    //更新
    void update(User user);

    Boolean checkUserEmail(String userEmail);

    UserDTO getUserInfoById(Integer userId);

    Result updatePassword(PasswordDTO passwordDTO);
}
