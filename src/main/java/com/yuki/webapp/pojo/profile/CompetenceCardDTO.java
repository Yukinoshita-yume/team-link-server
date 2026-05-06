package com.yuki.webapp.pojo.profile;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 前端能力卡片所需数据：技能标签、五维雷达、擅长方向、时间可用性热力图等。
 * <p>
 * 持久化在 Qdrant 的 payload 中（JSON 序列化），向量用于画像语义检索。
 */
@Data
public class CompetenceCardDTO {

    private Integer userId;

    private List<String> skillTags = new ArrayList<>();

    private RadarScoresDTO radarScores = new RadarScoresDTO();

    private Double totalScore;

    private List<String> expertiseAreas = new ArrayList<>();

    private Map<String, Object> availabilityHeatmap = new LinkedHashMap<>();

    private String llmSnapshot;
}
