package com.yuki.webapp.pojo.analysis;

import lombok.Data;

@Data
public class TextAnalysisResult {
    private TextPreprocessResult preprocess;
    private EntityExtractionResult entities;
    private ProfileImage profileImage;
    private FiveDimensionScore score;
}
