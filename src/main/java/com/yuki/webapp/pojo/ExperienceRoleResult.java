package com.yuki.webapp.pojo;

import lombok.Data;

import java.util.List;

/**
 * 经验断层 + 角色覆盖分析结果
 */
@Data
public class ExperienceRoleResult {

    private Integer totalExperienceCount;

    private boolean hasSimilarTypeExperience;

    private boolean hasLeaderExperience;

    private Double avgCompetitionScore;

    private String experienceGapDescription;

    private List<RoleCoverage> roleCoverages;

    @Data
    public static class RoleCoverage {
        private String roleName;

        private String status;

        private List<String> coveredByMembers;

        private String remark;
    }
}
