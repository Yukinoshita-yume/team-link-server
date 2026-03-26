package com.yuki.webapp.service.impl;

import com.yuki.webapp.mapper.MessageMapper;
import com.yuki.webapp.pojo.*;
import com.yuki.webapp.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    private MessageMapper messageMapper;

    @Override
    public void createMessage(Message message) {
        message.setIsRead(false);
        message.setMessageCreatedTime(LocalDateTime.now());
        messageMapper.createMessage(message);
    }

    @Override
    public Result<List<MessageDTO>> getMemberMessage(Integer userId, Boolean isRead) {
        try{
            List<MessageDTO> messages = messageMapper.getMemberMessage(userId,isRead);
            return Result.success(messages);
        }catch (Exception e){
            return Result.error("查询消息失败：" + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getUnadmittedMembers(Integer userId) {
        // 查询一个用户创建的所有竞赛
        List<Competition> competitions = messageMapper.getCreatedCompetitions(userId);
        // 为每个竞赛查询未录取成员
        List<Map<String, Object>> result = new ArrayList<>();
        for (Competition competition : competitions) {
            // 获取该竞赛所有未录取成员的列表（包含 userId 和 createdTime）
            List<CompetitionMember> members = messageMapper
                    .getUnadmittedUserId(competition.getCompetitionId());

            if (!members.isEmpty()) {
                // 提取userId用于批量查询用户名
                List<Integer> userIds = members.stream()
                        .map(CompetitionMember::getUserId)
                        .collect(Collectors.toList());

                // 批量查询这些用户的用户名
                List<User> users = messageMapper.getUserNameByIds(userIds);

                // 构建结果
                for (int i = 0; i < members.size(); i++) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("title", competition.getTitle());
                    item.put("userName", users.get(i).getUserName());
                    item.put("timestamp", members.get(i).getCompetitionMemberCreatedTime()); // 使用查询到的 createdTime
                    item.put("userId", members.get(i).getUserId());
                    item.put("competitionId", competition.getCompetitionId());
                    result.add(item);
                }
            }
        }
        return result;
    }

    @Override
    public void read(Integer messageId) {
        messageMapper.read(messageId);
    }
}
