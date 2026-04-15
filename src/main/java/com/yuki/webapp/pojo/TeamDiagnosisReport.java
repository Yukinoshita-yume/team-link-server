package com.yuki.webapp.pojo;

import lombok.Data;

/**
 * F4 队伍诊断完整报告
 */
@Data
public class TeamDiagnosisReport {

    /** 竞赛ID */
    private Integer competitionId;

    /** 竞赛名称 */
    private String competitionTitle;

    /** 诊断总分（0-100） */
    private Integer totalScore;

    /** 风险等级：LOW / MEDIUM / HIGH */
    private String riskLevel;

    /** 4.1 技能缺口分析 */
    private SkillGapResult skillGap;

    /** 4.2 时间冲突检测 */
    private TimeConflictResult timeConflict;

    /** 4.3 经验断层 + 角色覆盖 */
    private ExperienceRoleResult experienceRole;

    /** LLM 生成的综合优化建议（自然语言） */
    private String aiSuggestion;

    /** 报告生成耗时（毫秒） */
    private Long latencyMs;
}
