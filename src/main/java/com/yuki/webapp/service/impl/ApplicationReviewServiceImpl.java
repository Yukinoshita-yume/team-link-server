package com.yuki.webapp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yuki.webapp.mapper.CompetitionMapper;
import com.yuki.webapp.mapper.UserProfileMapper;
import com.yuki.webapp.mapper.UserSkillTagQueryMapper;
import com.yuki.webapp.pojo.CompetitionDetail;
import com.yuki.webapp.pojo.profile.UserProfileRecord;
import com.yuki.webapp.pojo.review.ApplicationAIReviewDTO;
import com.yuki.webapp.pojo.review.ReviewDimensionDTO;
import com.yuki.webapp.service.ApplicationReviewService;
import com.yuki.webapp.utils.DashScopeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApplicationReviewServiceImpl implements ApplicationReviewService {

    private static final double W_SKILL = 0.40;
    private static final double W_EXPERIENCE = 0.25;
    private static final double W_TIME = 0.20;
    private static final double W_TEAM = 0.15;

    // 本地缓存（带过期时间）
    private static final Map<String, CacheItem> CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MILLIS = 30L * 24 * 60 * 60 * 1000; // 30天

    @Autowired
    private UserProfileMapper userProfileMapper;
    @Autowired
    private UserSkillTagQueryMapper userSkillTagQueryMapper;
    @Autowired
    private CompetitionMapper competitionMapper;
    @Autowired
    private DashScopeUtil dashScopeUtil;

    @Override
    public ApplicationAIReviewDTO review(Integer competitionId, Integer applicantUserId) {

        String cacheKey = buildCacheKey(competitionId, applicantUserId);

        // 1️ 读取缓存
        CacheItem item = CACHE.get(cacheKey);
        if (item != null && !item.isExpired()) {
            return item.getData();
        }

        // 2️ 查询数据库
        CompetitionDetail competition = competitionMapper.competitionDetail(competitionId);
        UserProfileRecord profile = userProfileMapper.selectByUserId(applicantUserId);
        List<String> skillTags = userSkillTagQueryMapper.selectTagNamesByUserId(applicantUserId);

        // 3️ 构建结果
        ApplicationAIReviewDTO dto = buildReview(competitionId, applicantUserId, competition, profile, skillTags);

        // 4️ 写入缓存
        CACHE.put(cacheKey, new CacheItem(dto));

        return dto;
    }

    // 缓存对象（带过期时间）
    static class CacheItem {
        private final ApplicationAIReviewDTO data;
        private final long expireAt;

        public CacheItem(ApplicationAIReviewDTO data) {
            this.data = data;
            this.expireAt = System.currentTimeMillis() + CACHE_TTL_MILLIS;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }

        public ApplicationAIReviewDTO getData() {
            return data;
        }
    }

    private ApplicationAIReviewDTO buildReview(Integer competitionId, Integer applicantUserId,
                                               CompetitionDetail competition,
                                               UserProfileRecord profile,
                                               List<String> skillTags) {
        int skillScore = computeSkillScore(competition, skillTags);
        int experienceScore = zeroIfNull(profile == null ? null : profile.getScoreCompetition());
        int timeScore = zeroIfNull(profile == null ? null : profile.getScoreAvailability());
        int teamScore = zeroIfNull(profile == null ? null : profile.getScoreTeamwork());

        int total = clamp((int) Math.round(
                skillScore * W_SKILL
                        + experienceScore * W_EXPERIENCE
                        + timeScore * W_TIME
                        + teamScore * W_TEAM
        ));

        ApplicationAIReviewDTO dto = new ApplicationAIReviewDTO();
        dto.setCompetitionId(competitionId);
        dto.setUserId(applicantUserId);
        dto.setTotalScore(total);
        dto.setDecision(total >= 70 ? "建议通过" : "建议谨慎");
        dto.setDimensions(List.of(
                new ReviewDimensionDTO("技能匹配", skillScore),
                new ReviewDimensionDTO("经验契合", experienceScore),
                new ReviewDimensionDTO("时间匹配", timeScore),
                new ReviewDimensionDTO("团队互补", teamScore)
        ));

        fillLlmText(dto, competition, profile, skillTags);

        if (dto.getHighlights().isEmpty()) {
            dto.setHighlights(defaultHighlights(total, skillTags));
        }
        if (dto.getRisks().isEmpty()) {
            dto.setRisks(defaultRisks(profile, timeScore));
        }
        if (dto.getInterviewQuestions().isEmpty()) {
            dto.setInterviewQuestions(defaultQuestions(skillTags));
        }
        return dto;
    }

    private void fillLlmText(ApplicationAIReviewDTO dto, CompetitionDetail competition,
                             UserProfileRecord profile, List<String> skillTags) {
        try {
            String prompt = """
                    你是队长审核助手。基于输入数据输出 JSON，不要输出其它文字：
                    {
                      "highlights": ["亮点1", "亮点2", "亮点3"],
                      "risks": ["风险1", "风险2"],
                      "interviewQuestions": ["问题1", "问题2", "问题3"]
                    }
                    """;

            JSONObject context = new JSONObject();
            context.put("review", dto);
            context.put("competition", competition);
            context.put("abilitySummary", profile == null ? null : profile.getAbilitySummary());
            context.put("skillTags", skillTags);

            String resp = dashScopeUtil.chat(prompt, context.toJSONString(), 0.4);

            JSONObject obj = parseJsonObject(resp);
            dto.setHighlights(toStringList(obj.getJSONArray("highlights")));
            dto.setRisks(toStringList(obj.getJSONArray("risks")));
            dto.setInterviewQuestions(toStringList(obj.getJSONArray("interviewQuestions")));

        } catch (Exception ignored) {
        }
    }

    private static JSONObject parseJsonObject(String raw) {
        if (raw == null) return new JSONObject();
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return JSON.parseObject(raw.substring(start, end + 1));
        }
        return new JSONObject();
    }

    private static List<String> toStringList(JSONArray arr) {
        if (arr == null) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            String s = arr.getString(i);
            if (s != null && !s.isBlank()) {
                list.add(s.trim());
            }
        }
        return list;
    }

    private static List<String> defaultHighlights(int total, List<String> skillTags) {
        List<String> list = new ArrayList<>();
        if (total >= 80) {
            list.add("综合匹配度较高，可优先进入沟通环节。");
        } else if (total >= 70) {
            list.add("基础能力与队伍需求基本契合。");
        } else {
            list.add("存在一定契合点，但仍需进一步核验。");
        }
        if (skillTags != null && !skillTags.isEmpty()) {
            list.add("已识别技能标签：" + String.join("、", skillTags.subList(0, Math.min(3, skillTags.size()))) + "。");
        }
        return list;
    }

    private static List<String> defaultRisks(UserProfileRecord profile, int timeScore) {
        List<String> list = new ArrayList<>();
        if (profile == null) {
            list.add("缺少完整画像数据，建议增加补充问答。");
            return list;
        }
        if (timeScore < 60) {
            list.add("时间投入评分偏低，需确认关键赛段可用性。");
        }
        if (list.isEmpty()) {
            list.add("当前风险较低，重点关注实际协作节奏匹配。");
        }
        return list;
    }

    private static List<String> defaultQuestions(List<String> skillTags) {
        List<String> list = new ArrayList<>();
        list.add("请结合项目经历说明你最擅长的技术贡献点。");
        if (skillTags != null && !skillTags.isEmpty()) {
            list.add("你在「" + skillTags.get(0) + "」上的代表性成果是什么？");
        }
        list.add("如果赛题需求变化，你会如何调整分工与实现方案？");
        return list;
    }

    private static String buildCacheKey(Integer competitionId, Integer userId) {
        return "application:ai-review:" + competitionId + ":" + userId;
    }

    private static int computeSkillScore(CompetitionDetail competition, List<String> skillTags) {
        Set<String> required = new LinkedHashSet<>();
        if (competition != null) {
            addTag(required, competition.getTag1());
            addTag(required, competition.getTag2());
            addTag(required, competition.getTag3());
            addTag(required, competition.getTag4());
            addTag(required, competition.getTag5());
        }
        if (required.isEmpty()) return 70;

        Set<String> own = new LinkedHashSet<>();
        if (skillTags != null) {
            for (String s : skillTags) {
                if (s != null && !s.isBlank()) {
                    own.add(s.trim().toLowerCase());
                }
            }
        }
        if (own.isEmpty()) return 30;

        int hit = 0;
        for (String req : required) {
            for (String mine : own) {
                if (mine.contains(req) || req.contains(mine)) {
                    hit++;
                    break;
                }
            }
        }
        double ratio = (double) hit / required.size();
        return clamp((int) Math.round(30 + ratio * 70));
    }

    private static void addTag(Set<String> target, String tag) {
        if (tag != null && !tag.isBlank()) {
            target.add(tag.trim().toLowerCase());
        }
    }

    private static int zeroIfNull(Integer v) {
        return v == null ? 0 : v;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}