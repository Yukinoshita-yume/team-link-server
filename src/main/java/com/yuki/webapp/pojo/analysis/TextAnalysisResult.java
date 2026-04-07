package com.yuki.webapp.pojo.analysis;

import lombok.Data;

/**
 * 文本能力分析聚合结果：预处理、实体、画像、评分，以及面向前端的解释文案与质量标记。
 */
@Data
public class TextAnalysisResult {
    private TextPreprocessResult preprocess;
    private EntityExtractionResult entities;
    private ProfileImage profileImage;
    private FiveDimensionScore score;

    /**
     * 基于配置模板生成的综合评分说明（占位符已替换），可直接展示。
     */
    private String scoreExplanation;

    /**
     * 抽取/画像来源与置信度、低置信度提示等。
     */
    private AnalysisQuality quality;
}
