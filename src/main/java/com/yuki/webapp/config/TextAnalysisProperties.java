package com.yuki.webapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文本分析（预处理、实体、画像、五维评分）的可配置项。
 * <p>
 * 绑定前缀 {@code analysis.text}，便于在不改代码的情况下调整词典、权重与解释模板。
 */
@Data
@ConfigurationProperties(prefix = "analysis.text")
public class TextAnalysisProperties {

    /**
     * 五维综合分加权权重，五项之和建议为 1.0。
     */
    private Weights weights = new Weights();

    /**
     * 置信度阈值与分项基准分。
     */
    private Confidence confidence = new Confidence();

    /**
     * 规则兜底时使用的词典（大小写不敏感匹配，部分词条有特殊规则）。
     */
    private Dictionaries dictionaries = new Dictionaries();

    /**
     * 规则评分基数与增量系数。
     */
    private ScoringRules scoring = new ScoringRules();

    /**
     * 各维度关键词，用于规则打分与证据生成。
     */
    private Keywords keywords = new Keywords();

    /**
     * 面向前端的说明文案模板，支持占位符替换。
     */
    private Templates templates = new Templates();

    @Data
    public static class Weights {
        private double technicalDepth = 0.30;
        private double competitionExperience = 0.25;
        private double teamwork = 0.15;
        private double learningAbility = 0.15;
        private double timeCommitment = 0.15;
    }

    @Data
    public static class Confidence {
        /**
         * 低于该值时 {@link com.yuki.webapp.pojo.analysis.AnalysisQuality#lowConfidence} 为 true。
         */
        private double lowThreshold = 0.45;

        private double llmBoth = 0.88;
        private double llmEntityOnly = 0.62;
        private double llmProfileOnly = 0.58;
        private double ruleBoth = 0.32;

        /**
         * 清洗后文本长度小于等于该值时施加 {@link #shortTextPenalty}。
         */
        private int shortTextMaxLength = 30;

        private double shortTextPenalty = 0.12;
    }

    @Data
    public static class Dictionaries {
        private List<String> languages = new ArrayList<>(Arrays.asList(
                "java", "python", "c", "c++", "go", "rust", "javascript", "typescript", "kotlin", "swift", "php", "sql"
        ));
        private List<String> frameworks = new ArrayList<>(Arrays.asList(
                "spring", "springboot", "spring boot", "vue", "react", "angular", "flask", "fastapi",
                "django", "mybatis", "redis", "mysql", "qdrant", "meilisearch"
        ));
        private List<String> awards = new ArrayList<>(Arrays.asList(
                "国一", "国二", "国三", "省一", "省二", "省三", "一等奖", "二等奖", "三等奖", "金奖", "银奖", "铜奖", "top", "冠军"
        ));
    }

    @Data
    public static class ScoringRules {
        private int technicalBase = 40;
        private int technicalPerLanguage = 8;
        private int technicalPerFramework = 6;

        private int competitionBase = 35;
        private int competitionPerAward = 15;
        private int competitionPerKeywordHit = 5;

        private int teamworkBase = 45;
        private int teamworkPerKeywordHit = 10;

        private int learningBase = 45;
        private int learningPerKeywordHit = 10;

        private int timeBase = 40;
        private int timePerKeywordHit = 8;
    }

    @Data
    public static class Keywords {
        private List<String> competition = new ArrayList<>(Arrays.asList("竞赛", "比赛", "项目"));
        private List<String> teamwork = new ArrayList<>(Arrays.asList("团队", "协作", "组队", "沟通"));
        private List<String> learning = new ArrayList<>(Arrays.asList("学习", "自学", "复盘", "成长"));
        private List<String> timeCommitment = new ArrayList<>(Arrays.asList("每周", "投入", "时间", "长期", "坚持"));
    }

    @Data
    public static class Templates {
        /**
         * 占位符：{total} {td} {ce} {tw} {lr} {tm}
         */
        private String scoreExplanation = "综合得分 {total} 分。五维明细：技术深度 {td}、竞赛经验 {ce}、团队协作 {tw}、学习能力 {lr}、时间投入 {tm}。";

        private String fallbackExperienceSummary = "候选人具备基础技术栈和竞赛相关经历，建议结合项目细节进一步评估。";

        private String lowConfidenceHint = "当前结果较多依赖规则匹配或文本较短，置信度偏低，建议补充更具体的项目与竞赛经历。";

        private String teamworkFallbackEvidence = "文本中出现团队协作相关关键词。";

        private String learningFallbackEvidence = "文本中出现学习能力相关关键词。";

        private String timeFallbackEvidence = "文本中出现时间投入相关关键词。";
    }
}
