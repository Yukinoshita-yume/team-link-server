package com.yuki.webapp.pojo;

import lombok.Data;

@Data
public class MessageDTO extends Message{
    private String userName;
    private String title;
}
