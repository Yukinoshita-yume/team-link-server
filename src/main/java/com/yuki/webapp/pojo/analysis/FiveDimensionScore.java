package com.yuki.webapp.pojo.analysis;

import lombok.Data;

@Data
public class FiveDimensionScore {
    private ScoreDimension technicalDepth;
    private ScoreDimension competitionExperience;
    private ScoreDimension teamwork;
    private ScoreDimension learningAbility;
    private ScoreDimension timeCommitment;
    private Double totalScore;
}
