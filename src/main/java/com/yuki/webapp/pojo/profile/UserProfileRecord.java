package com.yuki.webapp.pojo.profile;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应 MySQL 表 {@code user_profile} 的核心字段（能力卡片展示所需）。
 */
@Data
public class UserProfileRecord {
    private Integer profileId;
    private Integer userId;

    private Integer scoreTechDepth;
    private Integer scoreCompetition;
    private Integer scoreTeamwork;
    private Integer scoreLearning;
    private Integer scoreAvailability;

    /** MySQL GENERATED COLUMN: composite_score（读取即可） */
    private Integer compositeScore;

    private String abilitySummary;
    private Integer weeklyHours;
    private String availablePeriods;
    private String busyLevel;

    private String rawInputSnapshot;
    private String llmOutputSnapshot;

    private LocalDateTime generatedAt;
}

