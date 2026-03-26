package com.yuki.webapp.mapper;

import com.yuki.webapp.pojo.*;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper {
    List<MessageDTO> getMemberMessage(
            @Param("userId") Integer userId,
            @Param("isRead") Boolean isRead
    );

    // 根据userId查询其创建的所有竞赛
    @Select("select competition_id, title from competition where user_id = #{userId}")
    List<Competition> getCreatedCompetitions(Integer userId);

    // 根据competitionId和admissionStatus查询所有未录取成员的userId
    @Select("select user_id, competition_member_created_time from competition_member " +
            "where competition_id = #{competitionId} and admission_status = 0")
    List<CompetitionMember> getUnadmittedUserId(Integer competitionId);

    // 批量查询用户姓名
    List<User> getUserNameByIds(@Param("userIds") List<Integer> userIds);

    // 创建信息
    @Insert("insert into message (user_id, competition_id, message_type, message_content, is_read, message_created_time) " +
            "values (#{userId}, #{competitionId}, #{messageType}, #{messageContent}, #{isRead}, #{messageCreatedTime})")
    @Options(useGeneratedKeys = true, keyProperty = "messageId")//设置messageId为自增
    void createMessage(Message message);

    // 标记为已读
    @Update("update message set is_read = true where message_id = #{messageId}")
    void read(Integer messageId);

    //创建队伍解散消息
    @Insert("insert into message (user_id, competition_id, message_type, message_content, is_read, message_created_time) " +
            "values (#{userId}, #{competitionId}, #{messageType}, #{messageContent}, #{isRead}, #{messageCreatedTime})")
    @Options(useGeneratedKeys = true, keyProperty = "messageId")
    void insertMessage(Message message);
}
