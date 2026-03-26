package com.yuki.webapp.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Competition {
    private Integer competitionId;
    private String title;
    private String tag1;
    private String tag2;
    private String tag3;
    private String tag4;
    private String tag5;
    private String competitionDetails;
    private Integer maxParticipants;
    private Integer userId;
    private String schoolRequirements;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;
    private LocalDateTime competitionCreatedTime;
    private LocalDateTime competitionUpdatedTime;
}
