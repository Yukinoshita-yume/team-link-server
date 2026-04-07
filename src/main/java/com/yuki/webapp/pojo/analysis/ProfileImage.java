package com.yuki.webapp.pojo.analysis;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 结构化画像字段，用于评分依据与前端展示。 */
@Data
public class ProfileImage {
    private List<String> skillTags = new ArrayList<>();
    private String experienceSummary;
    private String technicalDepthEvidence;
    private String competitionExperienceEvidence;
    private String teamworkEvidence;
    private String learningEvidence;
    private String timeCommitmentEvidence;
}
