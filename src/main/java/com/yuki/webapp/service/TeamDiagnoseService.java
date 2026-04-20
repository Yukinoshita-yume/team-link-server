package com.yuki.webapp.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yuki.webapp.mapper.CompetitionMapper;
import com.yuki.webapp.mapper.UserProfileMapper;
import com.yuki.webapp.mapper.UserSkillTagQueryMapper;
import com.yuki.webapp.pojo.CompetitionDetail;
import com.yuki.webapp.pojo.CompetitionUser;
import com.yuki.webapp.pojo.TeamDiagnoseDTO;
import com.yuki.webapp.pojo.profile.UserProfileRecord;
import com.yuki.webapp.utils.DashScopeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 队伍诊断服务
 * 基于竞赛信息 + 成员画像，调用 LLM 生成诊断报告
 * 执行时间目标 ≤5 秒
 */
@Service
public class TeamDiagnoseService {

    @Autowired
    private CompetitionMapper competitionMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private UserSkillTagQueryMapper userSkillTagQueryMapper;

    @Autowired
    private DashScopeUtil dashScopeUtil;

    /**
     * 对指定竞赛的队伍进行诊断，返回诊断报告
     *
     * @param competitionId 竞赛 ID
     * @return 诊断结果 DTO
     */
    public TeamDiagnoseDTO diagnose(Integer competitionId) {
        // 1. 查询竞赛基本信息
        CompetitionDetail detail = competitionMapper.competitionDetail(competitionId);
        if (detail == null) {
            TeamDiagnoseDTO empty = new TeamDiagnoseDTO();
            empty.setTotalScore(0);
            empty.setRiskLevel("HIGH");
            empty.getIssues().add("未找到该竞赛信息");
            empty.setSuggestions("请确认竞赛 ID 是否正确");
            return empty;
        }

        // 2. 查询已录取成员列表
        List<CompetitionUser> members = competitionMapper.getAdmittedMembers(competitionId);

        // 3. 收集成员画像数据
        List<Map<String, Object>> memberProfiles = new ArrayList<>();
        Map<String, String> timeConflictMap = new LinkedHashMap<>();

        for (CompetitionUser member : members) {
            Map<String, Object> profileData = new LinkedHashMap<>();
            profileData.put("userId", member.getUserId());
            profileData.put("userName", member.getUserName());

            UserProfileRecord record = userProfileMapper.selectByUserId(member.getUserId());
            List<String> tags = userSkillTagQueryMapper.selectTagNamesByUserId(member.getUserId());

            profileData.put("skillTags", tags);

            if (record != null) {
                profileData.put("scoreTechDepth", record.getScoreTechDepth());
                profileData.put("scoreCompetition", record.getScoreCompetition());
                profileData.put("scoreTeamwork", record.getScoreTeamwork());
                profileData.put("scoreLearning", record.getScoreLearning());
                profileData.put("scoreAvailability", record.getScoreAvailability());
                profileData.put("busyLevel", record.getBusyLevel());
                profileData.put("weeklyHours", record.getWeeklyHours());

                // 时间冲突热力图数据
                String busyLevel = record.getBusyLevel();
                if (busyLevel != null && !busyLevel.isBlank()) {
                    timeConflictMap.put(member.getUserName(), busyLevel);
                } else {
                    timeConflictMap.put(member.getUserName(), "NORMAL");
                }
            } else {
                timeConflictMap.put(member.getUserName(), "UNKNOWN");
            }
            memberProfiles.add(profileData);
        }

        // 4. 构建 LLM Prompt
        String competitionContext = buildCompetitionContext(detail);
        String membersContext = JSON.toJSONString(memberProfiles);
        String userPrompt = buildUserPrompt(competitionContext, membersContext, members.size(), detail.getMaxParticipants());

        String systemPrompt = """
                你是一位专业的竞赛队伍诊断专家。请根据竞赛信息和队伍成员画像，对队伍进行全面诊断。
                
                请严格按照以下 JSON 格式返回，不要有任何多余内容和 markdown 代码块标记：
                {
                  "totalScore": <0-100整数，综合评分>,
                  "riskLevel": "<LOW|MEDIUM|HIGH>",
                  "issues": ["<问题1>", "<问题2>", ...],
                  "suggestions": "<详细的自然语言优化建议，200字以内>",
                  "recruitTags": ["<需补招的技能标签1>", "<技能标签2>", ...],
                  "roleCoverage": {"<角色名>": <人数>, ...},
                  "skillGaps": [
                    {"skillName": "<技能名>", "reason": "<缺失原因>", "critical": <true|false>}
                  ]
                }
                
                评分规则：
                - 成员技能互补性占40分
                - 队伍时间可用性占20分
                - 竞赛经验匹配度占20分
                - 人员结构完整性占20分
                
                风险等级：totalScore>=70为LOW，40-69为MEDIUM，<40为HIGH
                """;

        // 5. 调用 LLM（设置较低 temperature 保证稳定输出）
        TeamDiagnoseDTO result = new TeamDiagnoseDTO();
        try {
            String llmResponse = dashScopeUtil.chat(systemPrompt, userPrompt, 0.2);
            result = parseLlmResponse(llmResponse);
        } catch (Exception e) {
            // LLM 调用失败时返回基于规则的基础诊断
            result = buildRuleBasedDiagnose(detail, members, memberProfiles);
        }

        // 6. 补充时间冲突热力图（本地计算，不依赖 LLM）
        result.setTimeConflictMap(timeConflictMap);

        return result;
    }

    private String buildCompetitionContext(CompetitionDetail detail) {
        List<String> tags = new ArrayList<>();
        if (detail.getTag1() != null && !detail.getTag1().isBlank()) tags.add(detail.getTag1());
        if (detail.getTag2() != null && !detail.getTag2().isBlank()) tags.add(detail.getTag2());
        if (detail.getTag3() != null && !detail.getTag3().isBlank()) tags.add(detail.getTag3());
        if (detail.getTag4() != null && !detail.getTag4().isBlank()) tags.add(detail.getTag4());
        if (detail.getTag5() != null && !detail.getTag5().isBlank()) tags.add(detail.getTag5());

        return String.format(
                "竞赛名称：%s\n竞赛标签：%s\n竞赛说明：%s\n最大人数：%d\n学校要求：%s",
                detail.getTitle(),
                String.join("、", tags),
                detail.getCompetitionDetails() != null ? detail.getCompetitionDetails() : "无",
                detail.getMaxParticipants() != null ? detail.getMaxParticipants() : 0,
                detail.getSchoolRequirements() != null ? detail.getSchoolRequirements() : "无"
        );
    }

    private String buildUserPrompt(String competitionContext, String membersContext, int currentCount, Integer maxParticipants) {
        return String.format(
                "【竞赛信息】\n%s\n\n【当前队伍成员（%d/%d人）】\n%s\n\n请对该队伍进行全面诊断，输出 JSON 格式的诊断报告。",
                competitionContext, currentCount,
                maxParticipants != null ? maxParticipants : 0,
                membersContext
        );
    }

    private TeamDiagnoseDTO parseLlmResponse(String llmResponse) {
        try {
            // 清理可能的 markdown 代码块
            String cleaned = llmResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```[a-zA-Z]*\\n?", "").replaceAll("```", "").trim();
            }

            JSONObject json = JSON.parseObject(cleaned);
            TeamDiagnoseDTO dto = new TeamDiagnoseDTO();

            dto.setTotalScore(json.getInteger("totalScore"));
            dto.setRiskLevel(json.getString("riskLevel"));
            dto.setSuggestions(json.getString("suggestions"));

            // 解析 issues
            JSONArray issuesArr = json.getJSONArray("issues");
            if (issuesArr != null) {
                dto.setIssues(issuesArr.toJavaList(String.class));
            }

            // 解析 recruitTags
            JSONArray tagsArr = json.getJSONArray("recruitTags");
            if (tagsArr != null) {
                dto.setRecruitTags(tagsArr.toJavaList(String.class));
            }

            // 解析 roleCoverage
            JSONObject roleObj = json.getJSONObject("roleCoverage");
            if (roleObj != null) {
                Map<String, Integer> roleMap = new LinkedHashMap<>();
                for (String key : roleObj.keySet()) {
                    roleMap.put(key, roleObj.getInteger(key));
                }
                dto.setRoleCoverage(roleMap);
            }

            // 解析 skillGaps
            JSONArray gapsArr = json.getJSONArray("skillGaps");
            if (gapsArr != null) {
                List<TeamDiagnoseDTO.SkillGap> gaps = new ArrayList<>();
                for (int i = 0; i < gapsArr.size(); i++) {
                    JSONObject gapObj = gapsArr.getJSONObject(i);
                    TeamDiagnoseDTO.SkillGap gap = new TeamDiagnoseDTO.SkillGap();
                    gap.setSkillName(gapObj.getString("skillName"));
                    gap.setReason(gapObj.getString("reason"));
                    gap.setCritical(Boolean.TRUE.equals(gapObj.getBoolean("critical")));
                    gaps.add(gap);
                }
                dto.setSkillGaps(gaps);
            }

            return dto;
        } catch (Exception e) {
            TeamDiagnoseDTO fallback = new TeamDiagnoseDTO();
            fallback.setTotalScore(50);
            fallback.setRiskLevel("MEDIUM");
            fallback.getIssues().add("诊断数据解析失败，请重试");
            fallback.setSuggestions(llmResponse);
            return fallback;
        }
    }

    /**
     * LLM 不可用时，基于规则生成基础诊断报告
     */
    private TeamDiagnoseDTO buildRuleBasedDiagnose(CompetitionDetail detail,
                                                    List<CompetitionUser> members,
                                                    List<Map<String, Object>> memberProfiles) {
        TeamDiagnoseDTO dto = new TeamDiagnoseDTO();

        int memberCount = members.size();
        int maxCount = detail.getMaxParticipants() != null ? detail.getMaxParticipants() : 5;

        // 基础评分
        int score = 60;
        List<String> issues = new ArrayList<>();

        if (memberCount == 0) {
            score = 20;
            issues.add("队伍暂无成员，无法有效参赛");
        } else if (memberCount < maxCount / 2) {
            score -= 20;
            issues.add("当前成员数量不足，建议继续招募");
        }

        // 技能覆盖检查
        List<String> allTags = new ArrayList<>();
        for (Map<String, Object> p : memberProfiles) {
            Object tagsObj = p.get("skillTags");
            if (tagsObj instanceof List<?> tags) {
                for (Object tag : tags) {
                    allTags.add(tag.toString());
                }
            }
        }

        if (allTags.isEmpty()) {
            score -= 15;
            issues.add("成员均未完善个人技能简介，无法评估技能结构");
        }

        dto.setTotalScore(Math.max(0, Math.min(100, score)));
        dto.setRiskLevel(score >= 70 ? "LOW" : score >= 40 ? "MEDIUM" : "HIGH");
        dto.setIssues(issues);
        dto.setSuggestions("建议所有成员完善个人技能简介，以便系统自动匹配优化建议。当前队伍规模" +
                memberCount + "/" + maxCount + "人，" +
                (memberCount < maxCount ? "仍有 " + (maxCount - memberCount) + " 个名额可继续招募。" : "名额已满。"));
        dto.setRecruitTags(List.of());

        return dto;
    }
}
