package com.yuki.webapp.pojo.analysis;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述实体抽取、画像生成来源与整体置信度，供前端展示提示或降级交互。
 */
@Data
public class AnalysisQuality {

    /**
     * 实体识别来源：{@code LLM} 表示模型 JSON 抽取成功；{@code RULE} 表示规则词典兜底。
     */
    private String entitySource;

    /**
     * 结构化画像来源：{@code LLM} 或 {@code RULE}。
     */
    private String profileSource;

    /**
     * 综合置信度，范围约 0~1，由配置项中的基准分与文本长度等因素组合得到。
     */
    private double overallConfidence;

    /**
     * 当 {@code overallConfidence} 低于配置的 {@code analysis.text.confidence.low-threshold} 时为 true。
     */
    private boolean lowConfidence;

    /**
     * 面向用户或前端的简短提示列表（如低置信度说明）。
     */
    private List<String> hints = new ArrayList<>();
}
