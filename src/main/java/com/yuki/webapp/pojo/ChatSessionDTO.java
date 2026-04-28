package com.yuki.webapp.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信会话摘要 DTO，用于会话列表展示
 */
@Data
public class ChatSessionDTO {
    private Integer otherUserId;       // 对方用户 ID
    private String  otherUserName;     // 对方用户名
    private String  lastMessage;       // 最后一条消息内容
    private Integer unreadCount;       // 未读数
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTime;    // 最后消息时间
}
