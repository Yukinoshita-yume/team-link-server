package com.yuki.webapp.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 队伍诊断报告结果 DTO
 * POST /api/teams/{id}/diagnose 返回此对象
 */
@Data
public class TeamDiagnoseDTO {

    /** 诊断综合分 0~100 */
    private Integer totalScore;

    /** 风险等级: LOW / MEDIUM / HIGH */
    private String riskLevel;

    /** 问题清单（最多 5 条） */
    private List<String> issues = new ArrayList<>();

    /** 优化建议（LLM 生成自然语言） */
    private String suggestions;

    /** 缺失技能的补招标签列表 */
    private List<String> recruitTags = new ArrayList<>();

    /** 角色覆盖情况 key=角色名 value=已有人数 */
    private Map<String, Integer> roleCoverage = new LinkedHashMap<>();

    /** 时间冲突热力图：key=成员名，value=繁忙程度描述 */
    private Map<String, String> timeConflictMap = new LinkedHashMap<>();

    /** 技能缺口列表（结构化，供前端展开显示） */
    private List<SkillGap> skillGaps = new ArrayList<>();

    @Data
    public static class SkillGap {
        private String skillName;
        private String reason;
        private boolean critical;
    }
}
