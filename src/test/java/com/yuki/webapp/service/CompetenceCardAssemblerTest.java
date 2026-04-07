package com.yuki.webapp.service;

import com.yuki.webapp.pojo.analysis.EntityExtractionResult;
import com.yuki.webapp.pojo.analysis.FiveDimensionScore;
import com.yuki.webapp.pojo.analysis.ProfileImage;
import com.yuki.webapp.pojo.analysis.ScoreDimension;
import com.yuki.webapp.pojo.analysis.TextAnalysisResult;
import com.yuki.webapp.pojo.profile.CompetenceCardDTO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompetenceCardAssemblerTest {

    @Test
    void fromTextAnalysis_mapsScoresAndTags() {
        TextAnalysisResult r = new TextAnalysisResult();
        ProfileImage p = new ProfileImage();
        p.setSkillTags(Arrays.asList("Java", "Vue"));
        r.setProfileImage(p);

        EntityExtractionResult e = new EntityExtractionResult();
        e.setLanguages(Arrays.asList("java"));
        e.setFrameworks(Arrays.asList("spring"));
        r.setEntities(e);

        FiveDimensionScore s = new FiveDimensionScore();
        s.setTechnicalDepth(new ScoreDimension(80, ""));
        s.setCompetitionExperience(new ScoreDimension(70, ""));
        s.setTeamwork(new ScoreDimension(60, ""));
        s.setLearningAbility(new ScoreDimension(75, ""));
        s.setTimeCommitment(new ScoreDimension(65, ""));
        s.setTotalScore(71.2);
        r.setScore(s);

        CompetenceCardDTO dto = CompetenceCardAssembler.fromTextAnalysis(r, 42);

        assertEquals(42, dto.getUserId());
        assertEquals(71.2, dto.getTotalScore());
        assertEquals(80, dto.getRadarScores().getTechnicalDepth());
        assertTrue(dto.getExpertiseAreas().contains("java"));
        assertTrue(dto.getLlmSnapshot().contains("technicalDepth"));
    }
}
