package com.yuki.webapp.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * RAG 搜索结果 POJO
 * 包含竞赛基本信息 + 匹配分 + 命中标签 + 推荐理由
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionSearchResult {

    // ── 竞赛基本信息（与 Competition 表字段对应）──
    private Integer competitionId;
    private String title;
    private String tag1;
    private String tag2;
    private String tag3;
    private String tag4;
    private String tag5;
    private String competitionDetails;
    private Integer maxParticipants;
    private String schoolRequirements;
    private String deadline;

    // ── RAG 增强字段 ──
    /** 匹配分 0-100，保留一位小数 */
    private Double matchScore;

    /** 命中的技能标签（用户查询中出现的标签） */
    private List<String> hitTags;

    /** LLM 生成的 1-2 句推荐理由 */
    private String recommendation;
}