package com.yuki.webapp.pojo.profile;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 前端「能力卡片」所需数据：技能标签、五维雷达、擅长方向、时间可用性热力图等。
 * <p>
 * 持久化在 Qdrant 的 payload 中（JSON 序列化），向量用于画像语义检索。
 */
@Data
public class CompetenceCardDTO {

    private Integer userId;

    private List<String> skillTags = new ArrayList<>();

    private RadarScoresDTO radarScores = new RadarScoresDTO();

    /** 综合分 0~100，可与五维加权一致 */
    private Double totalScore;

    /** 擅长方向 / 竞赛方向等 */
    private List<String> expertiseAreas = new ArrayList<>();

    /**
     * 时间可用性热力图：结构由前端约定，例如按周几、小时段二维数组，后端原样存储。
     */
    private Map<String, Object> availabilityHeatmap = new LinkedHashMap<>();

    /** LLM 生成快照或备注，便于审计与回放 */
    private String llmSnapshot;
}
