package com.yuki.webapp.service;

import com.alibaba.fastjson.JSON;
import com.yuki.webapp.mapper.SkillTagMapper;
import com.yuki.webapp.mapper.UserMapper;
import com.yuki.webapp.mapper.UserProfileMapper;
import com.yuki.webapp.mapper.UserSkillTagMapper;
import com.yuki.webapp.mapper.UserSkillTagQueryMapper;
import com.yuki.webapp.pojo.UserDTO;
import com.yuki.webapp.pojo.analysis.EntityExtractionResult;
import com.yuki.webapp.pojo.analysis.ProfileImage;
import com.yuki.webapp.pojo.analysis.TextAnalysisResult;
import com.yuki.webapp.pojo.profile.CompetenceCardDTO;
import com.yuki.webapp.pojo.profile.RadarScoresDTO;
import com.yuki.webapp.pojo.profile.UserProfileRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户画像（能力卡片）落库到 MySQL（见 static/ai_profile_schema.sql）。\n
 * 优先从 user_profile / user_skill_tag 读取；不存在时从 user.user_information 自动生成并写入。\n
 * user_skill_tag 为一对多：同一 user_id 可对应多行（每行一个 tag_id）。
 */
@Service
public class UserProfileMySqlService {

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private UserSkillTagMapper userSkillTagMapper;

    @Autowired
    private UserSkillTagQueryMapper userSkillTagQueryMapper;

    @Autowired
    private SkillTagMapper skillTagMapper;

    /** 仅用 Mapper 读 user.user_information，避免与 {@link UserService} 形成循环依赖 */
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TextAnalysisService textAnalysisService;

    public CompetenceCardDTO getOrGenerateCompetenceCard(int userId) {
        UserProfileRecord record = userProfileMapper.selectByUserId(userId);
        if (record == null) {
            return generateAndSaveFromIntro(userId);
        }
        return buildCompetenceCardFromDb(userId, record);
    }

    @Transactional(rollbackFor = Exception.class)
    public CompetenceCardDTO generateAndSaveFromIntro(int userId) {
        UserDTO user = userMapper.getUserInfoById(userId);
        if (user == null || user.getUserInformation() == null || user.getUserInformation().isBlank()) {
            clearProfileAndSkillTags(userId);
            CompetenceCardDTO empty = new CompetenceCardDTO();
            empty.setUserId(userId);
            empty.setRadarScores(new RadarScoresDTO());
            return empty;
        }
        String intro = user.getUserInformation();
        TextAnalysisResult analysis = textAnalysisService.analyze(intro);
        saveFromAnalysis(userId, intro, analysis);
        return getOrGenerateCompetenceCard(userId);
    }

    /**
     * 简介为空时删除该用户全部技能标签关联，并将画像主表重置，避免残留旧标签。
     */
    public void clearProfileAndSkillTags(int userId) {
        userSkillTagMapper.deleteByUserId(userId);
        userProfileMapper.upsertProfile(
                userId,
                0,
                0,
                0,
                0,
                0,
                null,
                0,
                null,
                "NORMAL",
                JSON.toJSONString(java.util.Map.of("intro", "")),
                null,
                LocalDateTime.now()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveFromAnalysis(int userId, String rawInput, TextAnalysisResult analysis) {
        if (analysis == null || analysis.getScore() == null) {
            return;
        }

        int tech = safeScore(analysis.getScore().getTechnicalDepth());
        int comp = safeScore(analysis.getScore().getCompetitionExperience());
        int team = safeScore(analysis.getScore().getTeamwork());
        int learn = safeScore(analysis.getScore().getLearningAbility());
        int time = safeScore(analysis.getScore().getTimeCommitment());

        ProfileImage profile = analysis.getProfileImage();
        String summary = profile != null ? profile.getExperienceSummary() : null;

        userProfileMapper.upsertProfile(
                userId,
                tech,
                comp,
                team,
                learn,
                time,
                summary,
                0,
                null,
                "NORMAL",
                JSON.toJSONString(java.util.Map.of("intro", rawInput)),
                JSON.toJSONString(analysis),
                LocalDateTime.now()
        );

        // 画像 skillTags + 实体抽取的语言/框架一并映射，保证一对多标签与简介内容一致
        userSkillTagMapper.deleteByUserId(userId);
        Set<Integer> uniqueTagIds = collectResolvedTagIds(profile, analysis.getEntities());
        for (Integer tagId : uniqueTagIds) {
            userSkillTagMapper.upsertOne(userId, tagId, 0.700, "从简介自动抽取");
        }
    }

    private Set<Integer> collectResolvedTagIds(ProfileImage profile, EntityExtractionResult entities) {
        Set<Integer> uniqueTagIds = new LinkedHashSet<>();
        List<String> skillTags = profile != null ? profile.getSkillTags() : List.of();
        for (String rawTag : skillTags) {
            for (String token : splitSkillTokens(rawTag)) {
                Integer tagId = resolveTagId(token);
                if (tagId != null) {
                    uniqueTagIds.add(tagId);
                }
            }
        }
        if (entities != null) {
            for (String s : entities.getLanguages()) {
                Integer tagId = resolveTagId(s);
                if (tagId != null) {
                    uniqueTagIds.add(tagId);
                }
            }
            for (String s : entities.getFrameworks()) {
                Integer tagId = resolveTagId(s);
                if (tagId != null) {
                    uniqueTagIds.add(tagId);
                }
            }
        }
        return uniqueTagIds;
    }

    private CompetenceCardDTO buildCompetenceCardFromDb(int userId, UserProfileRecord record) {
        CompetenceCardDTO dto = new CompetenceCardDTO();
        dto.setUserId(userId);

        RadarScoresDTO radar = new RadarScoresDTO();
        radar.setTechnicalDepth(zeroIfNull(record.getScoreTechDepth()));
        radar.setCompetitionExperience(zeroIfNull(record.getScoreCompetition()));
        radar.setTeamwork(zeroIfNull(record.getScoreTeamwork()));
        radar.setLearningAbility(zeroIfNull(record.getScoreLearning()));
        radar.setTimeCommitment(zeroIfNull(record.getScoreAvailability()));
        dto.setRadarScores(radar);

        dto.setTotalScore(record.getCompositeScore() != null ? record.getCompositeScore().doubleValue() : null);
        dto.setSkillTags(userSkillTagQueryMapper.selectTagNamesByUserId(userId));

        dto.getAvailabilityHeatmap().put("weeklyHours", record.getWeeklyHours());
        dto.getAvailabilityHeatmap().put("availablePeriods", record.getAvailablePeriods());
        dto.getAvailabilityHeatmap().put("busyLevel", record.getBusyLevel());

        dto.setLlmSnapshot(record.getLlmOutputSnapshot());
        return dto;
    }

    private static int safeScore(com.yuki.webapp.pojo.analysis.ScoreDimension d) {
        if (d == null || d.getScore() == null) return 0;
        return Math.max(0, Math.min(100, d.getScore()));
    }

    private static Integer zeroIfNull(Integer v) {
        return v == null ? 0 : v;
    }

    private Integer resolveTagId(String token) {
        if (token == null) return null;
        String t = token.trim();
        if (t.isBlank()) return null;

        Integer tagId = skillTagMapper.findTagIdByName(t);
        if (tagId != null) return tagId;
        tagId = skillTagMapper.findTagIdBySynonym(t);
        if (tagId != null) return tagId;
        tagId = skillTagMapper.findTagIdByAliasesLike(t);
        if (tagId != null) return tagId;
        return skillTagMapper.findTagIdByKeywordsLike(t);
    }

    private static List<String> splitSkillTokens(String raw) {
        if (raw == null) return List.of();
        String s = raw.trim();
        if (s.isBlank()) return List.of();
        // 兼容 LLM 输出如："Java, Spring Boot / MySQL"
        String[] parts = s.split("[,，/、|\\s]+");
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        for (String p : parts) {
            String x = p.trim();
            if (!x.isBlank()) list.add(x);
        }
        return list;
    }
}

