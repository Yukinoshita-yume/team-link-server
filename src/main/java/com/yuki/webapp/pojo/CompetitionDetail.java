package com.yuki.webapp.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionDetail {
    private Integer userId;
    private String title;
    private String tag1;
    private String tag2;
    private String tag3;
    private String tag4;
    private String tag5;
    private String competitionDetails;
    private Integer maxParticipants;
    private String schoolRequirements;
    private Date deadline;
    private Integer currentCount;
    private List<CompetitionUser> admittedMemberNames;
}
