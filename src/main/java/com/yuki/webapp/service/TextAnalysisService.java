package com.yuki.webapp.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class TextAnalysisService {

    @Autowired
    private DashScopeUtil dashScopeUtil;

    private static final List<String> LANGUAGE_DICT = Arrays.asList(
            "java", "python", "c", "c++", "go", "rust", "javascript", "typescript", "kotlin", "swift", "php", "sql"
    );

    private static final List<String> FRAMEWORK_DICT = Arrays.asList(
            "spring", "springboot", "spring boot", "vue", "react", "angular", "flask", "fastapi",
            "django", "mybatis", "redis", "mysql", "qdrant", "meilisearch"
    );

    private static final List<String> AWARD_DICT = Arrays.asList(
            "国一", "国二", "国三", "省一", "省二", "省三", "一等奖", "二等奖", "三等奖", "金奖", "银奖", "铜奖", "top", "冠军"
    );

    public TextAnalysisResult analyze(String inputText) {
        TextPreprocessResult preprocess = preprocess(inputText);
        EntityExtractionResult entities = extractEntities(preprocess.getCleanedText());
        ProfileImage profileImage = extractProfileImage(preprocess, entities);
        FiveDimensionScore score = score(profileImage, entities, preprocess.getCleanedText());

        TextAnalysisResult result = new TextAnalysisResult();
        result.setPreprocess(preprocess);
        result.setEntities(entities);
        result.setProfileImage(profileImage);
        result.setScore(score);
        return result;
    }

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

    private EntityExtractionResult extractEntities(String cleanedText) {
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
                return deduplicateEntity(parsed);
            }
        } catch (Exception ignored) {
        }

        return fallbackEntity(cleanedText);
    }

    private ProfileImage extractProfileImage(TextPreprocessResult preprocess, EntityExtractionResult entities) {
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
                return parsed;
            }
        } catch (Exception ignored) {
        }

        return fallbackProfile(preprocess, entities);
    }

    private FiveDimensionScore score(ProfileImage profile, EntityExtractionResult entities, String cleanedText) {
        FiveDimensionScore score = new FiveDimensionScore();

        int technicalDepthVal = clamp(40 + entities.getLanguages().size() * 8 + entities.getFrameworks().size() * 6);
        int competitionVal = clamp(35 + entities.getAwards().size() * 15 + countKeywords(cleanedText, Arrays.asList("竞赛", "比赛", "项目")) * 5);
        int teamworkVal = clamp(45 + countKeywords(cleanedText, Arrays.asList("团队", "协作", "组队", "沟通")) * 10);
        int learningVal = clamp(45 + countKeywords(cleanedText, Arrays.asList("学习", "自学", "复盘", "成长")) * 10);
        int timeVal = clamp(40 + countKeywords(cleanedText, Arrays.asList("每周", "投入", "时间", "长期", "坚持")) * 8);

        score.setTechnicalDepth(new ScoreDimension(technicalDepthVal, buildReason("技术深度", profile.getTechnicalDepthEvidence(), entities.getLanguages(), entities.getFrameworks())));
        score.setCompetitionExperience(new ScoreDimension(competitionVal, buildReason("竞赛经验", profile.getCompetitionExperienceEvidence(), entities.getAwards(), Collections.emptyList())));
        score.setTeamwork(new ScoreDimension(teamworkVal, buildReason("团队协作", profile.getTeamworkEvidence(), Collections.emptyList(), Collections.emptyList())));
        score.setLearningAbility(new ScoreDimension(learningVal, buildReason("学习能力", profile.getLearningEvidence(), Collections.emptyList(), Collections.emptyList())));
        score.setTimeCommitment(new ScoreDimension(timeVal, buildReason("时间投入", profile.getTimeCommitmentEvidence(), Collections.emptyList(), Collections.emptyList())));

        double total = technicalDepthVal * 0.30
                + competitionVal * 0.25
                + teamworkVal * 0.15
                + learningVal * 0.15
                + timeVal * 0.15;
        score.setTotalScore(Math.round(total * 10.0) / 10.0);
        return score;
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
        String lower = cleanedText == null ? "" : cleanedText.toLowerCase();
        EntityExtractionResult result = new EntityExtractionResult();
        result.setLanguages(matchDict(lower, LANGUAGE_DICT));
        result.setFrameworks(matchDict(lower, FRAMEWORK_DICT));
        result.setAwards(matchDict(lower, AWARD_DICT));
        return deduplicateEntity(result);
    }

    private ProfileImage fallbackProfile(TextPreprocessResult preprocess, EntityExtractionResult entities) {
        ProfileImage profile = new ProfileImage();

        Set<String> tags = new LinkedHashSet<>();
        tags.addAll(entities.getLanguages());
        tags.addAll(entities.getFrameworks());
        profile.setSkillTags(new ArrayList<>(tags));

        profile.setExperienceSummary("候选人具备基础技术栈和竞赛相关经历，建议结合项目细节进一步评估。");
        profile.setTechnicalDepthEvidence("识别到语言 " + String.join("、", entities.getLanguages()) + "，框架 " + String.join("、", entities.getFrameworks()) + "。");
        profile.setCompetitionExperienceEvidence("识别到奖项信息 " + String.join("、", entities.getAwards()) + "。");
        profile.setTeamworkEvidence(buildKeywordEvidence(preprocess.getCleanedText(), Arrays.asList("团队", "协作", "组队", "沟通"), "文本中出现团队协作相关关键词。"));
        profile.setLearningEvidence(buildKeywordEvidence(preprocess.getCleanedText(), Arrays.asList("学习", "自学", "复盘", "成长"), "文本中出现学习能力相关关键词。"));
        profile.setTimeCommitmentEvidence(buildKeywordEvidence(preprocess.getCleanedText(), Arrays.asList("每周", "投入", "时间", "长期", "坚持"), "文本中出现时间投入相关关键词。"));
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

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private int countKeywords(String text, List<String> words) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int count = 0;
        String lower = text.toLowerCase();
        for (String word : words) {
            if (lower.contains(word.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    private List<String> toStringList(JSONArray array) {
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

    private List<String> uniqueKeepOrder(List<String> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    private boolean isEntityEmpty(EntityExtractionResult entity) {
        return entity.getLanguages().isEmpty()
                && entity.getFrameworks().isEmpty()
                && entity.getAwards().isEmpty();
    }

    private List<String> matchDict(String source, List<String> dict) {
        List<String> hit = new ArrayList<>();
        for (String d : dict) {
            if (containsWord(source, d)) {
                hit.add(d);
            }
        }
        return hit;
    }

    private boolean containsWord(String source, String word) {
        if (source.contains(word)) {
            return true;
        }
        if ("c".equals(word)) {
            return Pattern.compile("(^|\\W)c(\\W|$)", Pattern.CASE_INSENSITIVE).matcher(source).find();
        }
        return false;
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new IllegalArgumentException("模型返回内容不是合法JSON对象");
        }
        return text.substring(start, end + 1);
    }
}
