package com.yuki.webapp.pojo.analysis;

import lombok.Data;

/** 五维分项得分与加权后的 {@code totalScore}（0~100）。 */
@Data
public class FiveDimensionScore {
    private ScoreDimension technicalDepth;
    private ScoreDimension competitionExperience;
    private ScoreDimension teamwork;
    private ScoreDimension learningAbility;
    private ScoreDimension timeCommitment;
    private Double totalScore;
}
