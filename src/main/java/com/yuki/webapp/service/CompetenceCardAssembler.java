package com.yuki.webapp.service;

import com.alibaba.fastjson.JSON;
import com.yuki.webapp.pojo.analysis.EntityExtractionResult;
import com.yuki.webapp.pojo.analysis.FiveDimensionScore;
import com.yuki.webapp.pojo.analysis.ProfileImage;
import com.yuki.webapp.pojo.analysis.ScoreDimension;
import com.yuki.webapp.pojo.analysis.TextAnalysisResult;
import com.yuki.webapp.pojo.profile.CompetenceCardDTO;
import com.yuki.webapp.pojo.profile.RadarScoresDTO;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将 {@link TextAnalysisResult} 转为 {@link CompetenceCardDTO}，便于写入 Qdrant 能力卡片。
 */
public final class CompetenceCardAssembler {

    private CompetenceCardAssembler() {
    }

    public static CompetenceCardDTO fromTextAnalysis(TextAnalysisResult analysis, int userId) {
        CompetenceCardDTO dto = new CompetenceCardDTO();
        dto.setUserId(userId);
        if (analysis == null) {
            dto.setRadarScores(new RadarScoresDTO());
            return dto;
        }

        ProfileImage profile = analysis.getProfileImage();
        if (profile != null && profile.getSkillTags() != null) {
            dto.setSkillTags(new ArrayList<>(profile.getSkillTags()));
        }

        FiveDimensionScore s = analysis.getScore();
        if (s != null) {
            RadarScoresDTO r = new RadarScoresDTO();
            r.setTechnicalDepth(scoreOf(s.getTechnicalDepth()));
            r.setCompetitionExperience(scoreOf(s.getCompetitionExperience()));
            r.setTeamwork(scoreOf(s.getTeamwork()));
            r.setLearningAbility(scoreOf(s.getLearningAbility()));
            r.setTimeCommitment(scoreOf(s.getTimeCommitment()));
            dto.setRadarScores(r);
            dto.setTotalScore(s.getTotalScore());
        } else {
            dto.setRadarScores(new RadarScoresDTO());
        }

        dto.setExpertiseAreas(buildExpertiseAreas(analysis.getEntities()));
        dto.setLlmSnapshot(JSON.toJSONString(analysis));

        return dto;
    }

    private static Integer scoreOf(ScoreDimension d) {
        return d == null || d.getScore() == null ? null : d.getScore();
    }

    private static List<String> buildExpertiseAreas(EntityExtractionResult entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        Set<String> set = new LinkedHashSet<>();
        addAll(set, entities.getLanguages());
        addAll(set, entities.getFrameworks());
        addAll(set, entities.getAwards());
        return new ArrayList<>(set);
    }

    private static void addAll(Set<String> set, List<String> list) {
        if (list == null) {
            return;
        }
        for (String s : list) {
            if (s != null && !s.isBlank()) {
                set.add(s.trim());
            }
        }
    }
}
