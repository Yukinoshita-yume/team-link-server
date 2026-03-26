package com.yuki.webapp.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionMember {
    private Integer competitionId;
    private Integer userId;
    private Boolean admissionStatus;
    private LocalDateTime competitionMemberCreatedTime;
    private LocalDateTime competitionMemberUpdatedTime;
}
