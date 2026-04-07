package com.yuki.webapp.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yuki.webapp.config.TextAnalysisProperties;
import com.yuki.webapp.pojo.analysis.AnalysisQuality;
import com.yuki.webapp.pojo.analysis.EntityExtractionResult;
import com.yuki.webapp.pojo.analysis.FiveDimensionScore;
import com.yuki.webapp.pojo.analysis.ProfileImage;
import com.yuki.webapp.pojo.analysis.ScoreDimension;
import com.yuki.webapp.pojo.analysis.TextAnalysisResult;
import com.yuki.webapp.pojo.analysis.TextPreprocessResult;
import com.yuki.webapp.utils.DashScopeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 文本分析流水线：预处理与分段、实体识别（LLM + 规则兜底）、结构化画像（LLM + 规则兜底）、
 * 五维规则评分、综合解释模板与置信度评估。
 * <p>
 * 配置项见 {@link TextAnalysisProperties}（前缀 {@code analysis.text}）。
 */
@Service
public class TextAnalysisService {

    private static final String SOURCE_LLM = "LLM";
    private static final String SOURCE_RULE = "RULE";

    @Autowired
    private DashScopeUtil dashScopeUtil;

    @Autowired
    private TextAnalysisProperties textAnalysisProperties;

    /**
     * 对输入文本执行完整分析并返回结构化结果。
     *
     * @param inputText 用户自述、简历片段或项目经历等原始文本
     * @return 预处理结果、实体、画像、评分、解释模板与质量信息；输入为 null 时按空串处理
     */
    public TextAnalysisResult analyze(String inputText) {
        TextPreprocessResult preprocess = preprocess(inputText);
        EntityStep entityStep = extractEntitiesWithSource(preprocess.getCleanedText());
        ProfileStep profileStep = extractProfileWithSource(preprocess, entityStep.entities());
        FiveDimensionScore score = buildFiveDimensionScore(profileStep.profile(), entityStep.entities(), preprocess.getCleanedText());
        AnalysisQuality quality = buildQuality(entityStep.fromLlm(), profileStep.fromLlm(), preprocess.getCleanedText());
        String scoreExplanation = buildScoreExplanation(score);

        TextAnalysisResult result = new TextAnalysisResult();
        result.setPreprocess(preprocess);
        result.setEntities(entityStep.entities());
        result.setProfileImage(profileStep.profile());
        result.setScore(score);
        result.setScoreExplanation(scoreExplanation);
        result.setQuality(quality);
        return result;
    }

    /**
     * 清洗空白、按句号类标点粗分段，便于后续抽取与展示。
     */
    private TextPreprocessResult preprocess(String inputText) {
        TextPreprocessResult result = new TextPreprocessResult();
        result.setOriginalText(inputText);

        String cleaned = inputText == null ? "" : inputText
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\t", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        result.setCleanedText(cleaned);

        if (cleaned.isBlank()) {
            result.setSegments(Collections.emptyList());
            return result;
        }

        String[] parts = cleaned.split("[。！？；;.!?]+");
        List<String> segments = new ArrayList<>();
        for (String part : parts) {
            String s = part.trim();
            if (!s.isBlank()) {
                segments.add(s);
            }
        }
        result.setSegments(segments);
        return result;
    }

    /**
     * 先调用 LLM 抽取实体；若失败或结果为空则使用配置词典规则匹配。
     */
    private EntityStep extractEntitiesWithSource(String cleanedText) {
        String systemPrompt = """
                你是信息抽取助手。请从文本中抽取三类实体：
                1) 编程语言 languages
                2) 技术框架 frameworks
                3) 竞赛奖项 awards
                只返回 JSON，格式：
                {"languages":["..."],"frameworks":["..."],"awards":["..."]}
                不要输出任何解释文字。
                """;

        try {
            String response = dashScopeUtil.chat(systemPrompt, cleanedText, 0.0);
            EntityExtractionResult parsed = parseEntityJson(response);
            if (!isEntityEmpty(parsed)) {
                return new EntityStep(deduplicateEntity(parsed), true);
            }
        } catch (Exception ignored) {
        }

        return new EntityStep(fallbackEntity(cleanedText), false);
    }

    /**
     * 基于文本与已抽取实体生成结构化画像；LLM 失败或 skillTags 为空时走规则模板。
     */
    private ProfileStep extractProfileWithSource(TextPreprocessResult preprocess, EntityExtractionResult entities) {
        String systemPrompt = """
                你是候选人画像助手。根据输入文本和已抽取实体，生成结构化画像 JSON。
                返回格式如下，不要额外输出：
                {
                  "skillTags": ["..."],
                  "experienceSummary": "...",
                  "technicalDepthEvidence": "...",
                  "competitionExperienceEvidence": "...",
                  "teamworkEvidence": "...",
                  "learningEvidence": "...",
                  "timeCommitmentEvidence": "..."
                }
                """;

        JSONObject userObj = new JSONObject();
        userObj.put("text", preprocess.getCleanedText());
        userObj.put("entities", JSON.toJSON(entities));

        try {
            String response = dashScopeUtil.chat(systemPrompt, userObj.toJSONString(), 0.1);
            ProfileImage parsed = parseProfileJson(response);
            if (parsed.getSkillTags() != null && !parsed.getSkillTags().isEmpty()) {
                return new ProfileStep(parsed, true);
            }
        } catch (Exception ignored) {
        }

        return new ProfileStep(fallbackProfile(preprocess, entities), false);
    }

    /**
     * 按配置中的基数、关键词命中次数与实体数量计算五维分数，并用配置权重合成总分。
     */
    private FiveDimensionScore buildFiveDimensionScore(ProfileImage profile, EntityExtractionResult entities, String cleanedText) {
        TextAnalysisProperties.ScoringRules s = textAnalysisProperties.getScoring();
        TextAnalysisProperties.Keywords k = textAnalysisProperties.getKeywords();

        int technicalDepthVal = clamp(s.getTechnicalBase()
                + entities.getLanguages().size() * s.getTechnicalPerLanguage()
                + entities.getFrameworks().size() * s.getTechnicalPerFramework());
        int competitionVal = clamp(s.getCompetitionBase()
                + entities.getAwards().size() * s.getCompetitionPerAward()
                + countKeywords(cleanedText, k.getCompetition()) * s.getCompetitionPerKeywordHit());
        int teamworkVal = clamp(s.getTeamworkBase()
                + countKeywords(cleanedText, k.getTeamwork()) * s.getTeamworkPerKeywordHit());
        int learningVal = clamp(s.getLearningBase()
                + countKeywords(cleanedText, k.getLearning()) * s.getLearningPerKeywordHit());
        int timeVal = clamp(s.getTimeBase()
                + countKeywords(cleanedText, k.getTimeCommitment()) * s.getTimePerKeywordHit());

        FiveDimensionScore score = new FiveDimensionScore();
        score.setTechnicalDepth(new ScoreDimension(technicalDepthVal, buildReason("技术深度", profile.getTechnicalDepthEvidence(), entities.getLanguages(), entities.getFrameworks())));
        score.setCompetitionExperience(new ScoreDimension(competitionVal, buildReason("竞赛经验", profile.getCompetitionExperienceEvidence(), entities.getAwards(), Collections.emptyList())));
        score.setTeamwork(new ScoreDimension(teamworkVal, buildReason("团队协作", profile.getTeamworkEvidence(), Collections.emptyList(), Collections.emptyList())));
        score.setLearningAbility(new ScoreDimension(learningVal, buildReason("学习能力", profile.getLearningEvidence(), Collections.emptyList(), Collections.emptyList())));
        score.setTimeCommitment(new ScoreDimension(timeVal, buildReason("时间投入", profile.getTimeCommitmentEvidence(), Collections.emptyList(), Collections.emptyList())));

        TextAnalysisProperties.Weights w = textAnalysisProperties.getWeights();
        double total = technicalDepthVal * w.getTechnicalDepth()
                + competitionVal * w.getCompetitionExperience()
                + teamworkVal * w.getTeamwork()
                + learningVal * w.getLearningAbility()
                + timeVal * w.getTimeCommitment();
        score.setTotalScore(Math.round(total * 10.0) / 10.0);
        return score;
    }

    /**
     * 根据 LLM/规则来源与文本长度计算置信度，并判断是否低置信度。
     */
    private AnalysisQuality buildQuality(boolean entityFromLlm, boolean profileFromLlm, String cleanedText) {
        TextAnalysisProperties.Confidence c = textAnalysisProperties.getConfidence();
        double base;
        if (entityFromLlm && profileFromLlm) {
            base = c.getLlmBoth();
        } else if (entityFromLlm) {
            base = c.getLlmEntityOnly();
        } else if (profileFromLlm) {
            base = c.getLlmProfileOnly();
        } else {
            base = c.getRuleBoth();
        }

        double overall = base;
        if (cleanedText != null && cleanedText.length() <= c.getShortTextMaxLength()) {
            overall = Math.max(0, overall - c.getShortTextPenalty());
        }
        overall = Math.min(1.0, Math.max(0.0, overall));

        AnalysisQuality q = new AnalysisQuality();
        q.setEntitySource(entityFromLlm ? SOURCE_LLM : SOURCE_RULE);
        q.setProfileSource(profileFromLlm ? SOURCE_LLM : SOURCE_RULE);
        q.setOverallConfidence(Math.round(overall * 1000.0) / 1000.0);
        q.setLowConfidence(overall < c.getLowThreshold());
        if (q.isLowConfidence()) {
            q.getHints().add(textAnalysisProperties.getTemplates().getLowConfidenceHint());
        }
        return q;
    }

    /**
     * 使用配置模板填充五维分数与总分，供前端直接展示。
     */
    private String buildScoreExplanation(FiveDimensionScore score) {
        String tpl = textAnalysisProperties.getTemplates().getScoreExplanation();
        if (tpl == null || tpl.isBlank()) {
            return "";
        }
        return tpl
                .replace("{total}", String.valueOf(score.getTotalScore()))
                .replace("{td}", formatScore(score.getTechnicalDepth()))
                .replace("{ce}", formatScore(score.getCompetitionExperience()))
                .replace("{tw}", formatScore(score.getTeamwork()))
                .replace("{lr}", formatScore(score.getLearningAbility()))
                .replace("{tm}", formatScore(score.getTimeCommitment()));
    }

    private static String formatScore(ScoreDimension d) {
        if (d == null || d.getScore() == null) {
            return "-";
        }
        return String.valueOf(d.getScore());
    }

    private EntityExtractionResult parseEntityJson(String response) {
        String json = extractJsonObject(response);
        JSONObject obj = JSON.parseObject(json);
        EntityExtractionResult result = new EntityExtractionResult();
        result.setLanguages(toStringList(obj.getJSONArray("languages")));
        result.setFrameworks(toStringList(obj.getJSONArray("frameworks")));
        result.setAwards(toStringList(obj.getJSONArray("awards")));
        return result;
    }

    private ProfileImage parseProfileJson(String response) {
        String json = extractJsonObject(response);
        JSONObject obj = JSON.parseObject(json);
        ProfileImage profile = new ProfileImage();
        profile.setSkillTags(toStringList(obj.getJSONArray("skillTags")));
        profile.setExperienceSummary(obj.getString("experienceSummary"));
        profile.setTechnicalDepthEvidence(obj.getString("technicalDepthEvidence"));
        profile.setCompetitionExperienceEvidence(obj.getString("competitionExperienceEvidence"));
        profile.setTeamworkEvidence(obj.getString("teamworkEvidence"));
        profile.setLearningEvidence(obj.getString("learningEvidence"));
        profile.setTimeCommitmentEvidence(obj.getString("timeCommitmentEvidence"));
        return profile;
    }

    private EntityExtractionResult fallbackEntity(String cleanedText) {
        TextAnalysisProperties.Dictionaries dict = textAnalysisProperties.getDictionaries();
        String lower = cleanedText == null ? "" : cleanedText.toLowerCase();
        EntityExtractionResult result = new EntityExtractionResult();
        result.setLanguages(matchDict(lower, dict.getLanguages()));
        result.setFrameworks(matchDict(lower, dict.getFrameworks()));
        result.setAwards(matchDict(lower, dict.getAwards()));
        return deduplicateEntity(result);
    }

    private ProfileImage fallbackProfile(TextPreprocessResult preprocess, EntityExtractionResult entities) {
        TextAnalysisProperties.Templates t = textAnalysisProperties.getTemplates();
        TextAnalysisProperties.Keywords k = textAnalysisProperties.getKeywords();
        ProfileImage profile = new ProfileImage();

        Set<String> tags = new LinkedHashSet<>();
        tags.addAll(entities.getLanguages());
        tags.addAll(entities.getFrameworks());
        profile.setSkillTags(new ArrayList<>(tags));

        profile.setExperienceSummary(t.getFallbackExperienceSummary());

        if (entities.getLanguages().isEmpty() && entities.getFrameworks().isEmpty()) {
            profile.setTechnicalDepthEvidence("未从文本中匹配到明确语言或框架关键词。");
        } else {
            profile.setTechnicalDepthEvidence("识别到语言 " + String.join("、", entities.getLanguages())
                    + "，框架 " + String.join("、", entities.getFrameworks()) + "。");
        }

        if (entities.getAwards().isEmpty()) {
            profile.setCompetitionExperienceEvidence("未从文本中匹配到明确奖项关键词。");
        } else {
            profile.setCompetitionExperienceEvidence("识别到奖项信息 " + String.join("、", entities.getAwards()) + "。");
        }

        profile.setTeamworkEvidence(buildKeywordEvidence(preprocess.getCleanedText(), k.getTeamwork(), t.getTeamworkFallbackEvidence()));
        profile.setLearningEvidence(buildKeywordEvidence(preprocess.getCleanedText(), k.getLearning(), t.getLearningFallbackEvidence()));
        profile.setTimeCommitmentEvidence(buildKeywordEvidence(preprocess.getCleanedText(), k.getTimeCommitment(), t.getTimeFallbackEvidence()));
        return profile;
    }

    private String buildReason(String dimension, String evidence, List<String> listA, List<String> listB) {
        StringBuilder sb = new StringBuilder();
        sb.append(dimension).append("评分依据：");
        if (evidence != null && !evidence.isBlank()) {
            sb.append(evidence);
        } else {
            sb.append("结合文本信息与结构化特征综合评估。");
        }
        if (!listA.isEmpty()) {
            sb.append(" 关键项：").append(String.join("、", listA)).append("。");
        }
        if (!listB.isEmpty()) {
            sb.append(" 补充项：").append(String.join("、", listB)).append("。");
        }
        return sb.toString();
    }

    private String buildKeywordEvidence(String text, List<String> words, String fallback) {
        List<String> hit = new ArrayList<>();
        String source = text == null ? "" : text.toLowerCase();
        for (String word : words) {
            if (source.contains(word.toLowerCase())) {
                hit.add(word);
            }
        }
        if (hit.isEmpty()) {
            return fallback;
        }
        return "命中关键词：" + String.join("、", hit) + "。";
    }

    private static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private static int countKeywords(String text, List<String> words) {
        if (text == null || text.isBlank() || words == null) {
            return 0;
        }
        int count = 0;
        String lower = text.toLowerCase();
        for (String word : words) {
            if (word != null && lower.contains(word.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    private static List<String> toStringList(JSONArray array) {
        if (array == null) {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            String s = array.getString(i);
            if (s != null) {
                s = s.trim();
            }
            if (s != null && !s.isBlank()) {
                list.add(s);
            }
        }
        return list;
    }

    private EntityExtractionResult deduplicateEntity(EntityExtractionResult result) {
        result.setLanguages(uniqueKeepOrder(result.getLanguages()));
        result.setFrameworks(uniqueKeepOrder(result.getFrameworks()));
        result.setAwards(uniqueKeepOrder(result.getAwards()));
        return result;
    }

    private static List<String> uniqueKeepOrder(List<String> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    private static boolean isEntityEmpty(EntityExtractionResult entity) {
        return entity.getLanguages().isEmpty()
                && entity.getFrameworks().isEmpty()
                && entity.getAwards().isEmpty();
    }

    private List<String> matchDict(String source, List<String> dict) {
        List<String> hit = new ArrayList<>();
        if (dict == null) {
            return hit;
        }
        for (String d : dict) {
            if (d != null && containsWord(source, d)) {
                hit.add(d);
            }
        }
        return hit;
    }

    private static boolean containsWord(String source, String word) {
        if (source.contains(word)) {
            return true;
        }
        if ("c".equals(word)) {
            return Pattern.compile("(^|\\W)c(\\W|$)", Pattern.CASE_INSENSITIVE).matcher(source).find();
        }
        return false;
    }

    private static String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new IllegalArgumentException("模型返回内容不是合法JSON对象");
        }
        return text.substring(start, end + 1);
    }

    private record EntityStep(EntityExtractionResult entities, boolean fromLlm) {
    }

    private record ProfileStep(ProfileImage profile, boolean fromLlm) {
    }
}
