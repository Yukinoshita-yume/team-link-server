package com.yuki.webapp.pojo.profile;

import lombok.Data;

/**
 * 能力卡片雷达图五维分数（0~100），与 {@link com.yuki.webapp.pojo.analysis.FiveDimensionScore} 语义对齐。
 */
@Data
public class RadarScoresDTO {
    private Integer technicalDepth;
    private Integer competitionExperience;
    private Integer teamwork;
    private Integer learningAbility;
    private Integer timeCommitment;
}
