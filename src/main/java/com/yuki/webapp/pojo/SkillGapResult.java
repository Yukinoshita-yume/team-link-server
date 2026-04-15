package com.yuki.webapp.pojo;

import lombok.Data;

import java.util.List;

/**
 * 4.1 技能缺口分析结果
 */
@Data
public class SkillGapResult {

    /** 严重缺口：竞赛核心技能无任何成员覆盖 */
    private List<GapItem> criticalGaps;

    /** 一般缺口：技能有覆盖但熟练度为初级 */
    private List<GapItem> moderateGaps;

    /** 轻微缺口：技能覆盖但人员单点依赖 */
    private List<GapItem> minorGaps;

    @Data
    public static class GapItem {
        /** 技能标签名称 */
        private String skillName;

        /** 缺口等级：CRITICAL / MODERATE / MINOR */
        private String gapLevel;

        /** 建议行动 */
        private String suggestion;

        /** 覆盖该技能的成员列表（为空则表示完全缺失） */
        private List<String> coveredByMembers;
    }
}
