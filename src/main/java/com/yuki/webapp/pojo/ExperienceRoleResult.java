package com.yuki.webapp.pojo;

import lombok.Data;

import java.util.List;

/**
 * 4.3 经验断层 + 角色覆盖分析结果
 */
@Data
public class ExperienceRoleResult {

    /** 队伍参赛经验总场次 */
    private Integer totalExperienceCount;

    /** 是否有人参加过同类型竞赛 */
    private boolean hasSimilarTypeExperience;

    /** 是否有人担任过队长/技术负责人 */
    private boolean hasLeaderExperience;

    /** 团队竞赛经验评分均值（来自 user_profile.score_competition，0-100） */
    private Double avgCompetitionScore;

    /** 经验断层风险描述 */
    private String experienceGapDescription;

    /** 角色覆盖情况列表 */
    private List<RoleCoverage> roleCoverages;

    @Data
    public static class RoleCoverage {
        /** 角色名称，如：技术开发、产品策划、设计、答辩表达 */
        private String roleName;

        /** 覆盖状态：COVERED / WEAK / MISSING */
        private String status;

        /** 覆盖该角色的成员名称 */
        private List<String> coveredByMembers;

        /** 备注，如"兼职覆盖" */
        private String remark;
    }
}
