package com.yuki.webapp.service;

import com.yuki.webapp.pojo.*;

import java.util.List;
import java.util.Map;

public interface MessageService {
    void createMessage(Message message);

    Result<List<MessageDTO>> getMemberMessage(Integer userId, Boolean isRead);

    List<Map<String, Object>> getUnadmittedMembers(Integer userId);

    List<Map<String, Object>> getUnadmittedMembersByCompetition(Integer competitionId);

    void read(Integer messageId);

    // 私信
    void sendDirectMessage(Integer senderId, Integer receiverId, String content);

    List<DirectMessageDTO> getConversation(Integer userId, Integer otherUserId);

    void markConversationRead(Integer userId, Integer senderId);

    List<ChatSessionDTO> getChatSessions(Integer userId);

    int getUnreadDirectMessageCount(Integer userId);
}
