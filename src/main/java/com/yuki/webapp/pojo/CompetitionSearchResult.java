package com.yuki.webapp.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * RAG 搜索结果
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
    private Double matchScore;

    private List<String> hitTags;

    private String recommendation;
}