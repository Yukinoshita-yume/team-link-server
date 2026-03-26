package com.yuki.webapp.service;

import com.yuki.webapp.pojo.Message;
import com.yuki.webapp.pojo.MessageDTO;
import com.yuki.webapp.pojo.Result;

import java.util.List;
import java.util.Map;

public interface MessageService {
    void createMessage(Message message);

    Result<List<MessageDTO>> getMemberMessage(Integer userId, Boolean isRead);

    List<Map<String, Object>> getUnadmittedMembers(Integer userId);

    void read(Integer messageId);
}
