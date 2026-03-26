package com.yuki.webapp.mapper;

import com.yuki.webapp.pojo.User;
import com.yuki.webapp.pojo.UserDTO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    //根据用户名查询用户
    @Select("select * from user where user_email=#{userEmail}")
    User findByUserEmail(String userEmail);

    //检查邮箱是否被占用
    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM user
                WHERE user_email = #{userEmail}
            );""")
    Boolean checkUserEmail(String userEmail);

    //添加
    @Insert("insert into user(user_name,user_password,user_email,user_registration_time,user_update_time)" +
            " values(#{userName},#{userPassword},#{userEmail},now(),now())")
    void add(String userName, String userPassword,String userEmail);

    @Update("update user set user_name=#{userName},user_update_time=#{userUpdateTime}," +
            "user_gender=#{userGender},user_university=#{userUniversity},user_major=#{userMajor}," +
            "user_information=#{userInformation} where user_id=#{userId}")
    void update(User user);

    // 根据userId查询用户详细信息
    @Select("select * from user where user_id = #{userId}")
    UserDTO getUserInfoById(Integer userId);

    // 查询旧密码
    @Select("select user_password from user where user_email = #{userEmail}")
    String selectPasswordByEmail(String userEmail);

    // 修改密码
    @Update("update user set user_password = #{newPassword} where user_email = #{userEmail}")
    int updatePasswordByEmail(@Param("userEmail") String userEmail,
                              @Param("newPassword") String newPassword);
}
