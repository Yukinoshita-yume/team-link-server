package com.yuki.webapp.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信消息 DTO
 */
@Data
public class DirectMessageDTO {
    private Integer messageId;
    private Integer senderId;
    private String  senderName;
    private Integer receiverId;
    private String  messageContent;
    private Boolean isRead;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime messageCreatedTime;
}
