package com.yuki.webapp.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionUser {
    private Integer userId;
    private String userName;
    private Integer admissionStatus; // 0: 未录取, 1: 已录取
}
