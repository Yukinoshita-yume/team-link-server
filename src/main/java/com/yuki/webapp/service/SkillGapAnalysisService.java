package com.yuki.webapp.service;

import com.yuki.webapp.mapper.TeamDiagnosisMapper;
import com.yuki.webapp.pojo.SkillGapResult;
import com.yuki.webapp.utils.DashScopeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 4.1 技能缺口分析服务
 *
 * 逻辑：
 * 1. 从竞赛的 tag1~tag5 提取所需技能列表
 * 2. 聚合全队成员的技能标签（含熟练度）
 * 3. 集合运算找出缺失 / 薄弱技能
 * 4. 对无法精确匹配的技能，用 Embedding 余弦相似度做近义词匹配（阈值 0.82）
 * 5. 按缺口等级分类输出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillGapAnalysisService {

    private final TeamDiagnosisMapper diagnosisMapper;
    private final DashScopeUtil dashScopeUtil;

    /** Embedding 相似度阈值：高于此值视为"技能相似" */
    private static final float SIMILARITY_THRESHOLD = 0.82f;

    /**
     * 分析技能缺口
     *
     * @param competitionInfo 竞赛基本信息（含 tag1~tag5）
     * @param allMemberIds    全队成员 userId 列表（含队长）
     * @return 技能缺口分析结果
     */
    public SkillGapResult analyze(Map<String, Object> competitionInfo, List<Integer> allMemberIds) {

        // Step 1: 提取竞赛所需技能
        List<String> requiredSkills = extractRequiredSkills(competitionInfo);
        log.info("[SkillGap] 竞赛所需技能: {}", requiredSkills);

        if (requiredSkills.isEmpty()) {
            SkillGapResult empty = new SkillGapResult();
            empty.setCriticalGaps(Collections.emptyList());
            empty.setModerateGaps(Collections.emptyList());
            empty.setMinorGaps(Collections.emptyList());
            return empty;
        }

        // Step 2: 聚合全队技能 Map<skillName_lower -> List<{userId, userName, level}>>
        Map<String, List<MemberSkillInfo>> teamSkillMap = aggregateTeamSkills(allMemberIds);
        log.info("[SkillGap] 全队技能数量: {}", teamSkillMap.size());

        // Step 3 + 4: 对每个所需技能做匹配判断
        List<SkillGapResult.GapItem> critical = new ArrayList<>();
        List<SkillGapResult.GapItem> moderate = new ArrayList<>();
        List<SkillGapResult.GapItem> minor    = new ArrayList<>();

        for (String requiredSkill : requiredSkills) {
            List<MemberSkillInfo> matched = findMatchedMembers(requiredSkill, teamSkillMap);

            SkillGapResult.GapItem item = new SkillGapResult.GapItem();
            item.setSkillName(requiredSkill);

            if (matched.isEmpty()) {
                // 严重缺口：完全没有覆盖
                item.setGapLevel("CRITICAL");
                item.setSuggestion("必须招募具备 [" + requiredSkill + "] 技能的成员，或寻求外部合作");
                item.setCoveredByMembers(Collections.emptyList());
                critical.add(item);

            } else {
                // 检查是否所有覆盖者都是初级
                boolean allBeginner = matched.stream()
                        .allMatch(m -> "BEGINNER".equals(m.level));
                // 检查是否只有一个人覆盖（单点依赖）
                boolean singlePoint = matched.size() == 1;

                List<String> memberNames = matched.stream()
                        .map(m -> m.userName)
                        .collect(Collectors.toList());

                if (allBeginner) {
                    // 一般缺口：有覆盖但熟练度低
                    item.setGapLevel("MODERATE");
                    item.setSuggestion("建议招募更高水平的 [" + requiredSkill + "] 人才，或对现有成员进行针对性培训");
                    item.setCoveredByMembers(memberNames);
                    moderate.add(item);

                } else if (singlePoint) {
                    // 轻微缺口：单点依赖
                    item.setGapLevel("MINOR");
                    item.setSuggestion("[" + requiredSkill + "] 存在单点依赖风险，建议安排至少一名成员交叉学习");
                    item.setCoveredByMembers(memberNames);
                    minor.add(item);

                } else {
                    // 无缺口，不加入列表
                    log.debug("[SkillGap] 技能 [{}] 覆盖良好，成员: {}", requiredSkill, memberNames);
                }
            }
        }

        SkillGapResult result = new SkillGapResult();
        result.setCriticalGaps(critical);
        result.setModerateGaps(moderate);
        result.setMinorGaps(minor);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────
    // 私有方法
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 从竞赛信息中提取非空技能标签
     */
    private List<String> extractRequiredSkills(Map<String, Object> competitionInfo) {
        List<String> skills = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Object tag = competitionInfo.get("tag" + i);
            if (tag != null && !tag.toString().isBlank()) {
                skills.add(tag.toString().trim());
            }
        }
        return skills;
    }

    /**
     * 聚合全队成员技能，返回 Map<skillName小写 -> List<成员技能信息>>
     */
    private Map<String, List<MemberSkillInfo>> aggregateTeamSkills(List<Integer> memberIds) {
        Map<String, List<MemberSkillInfo>> result = new HashMap<>();
        // 这里需要通过 userId 查到 userName，先查 admittedMembers 时已经有了
        // 此处简化：只存 userId，userName 在调用方已知
        for (Integer userId : memberIds) {
            List<Map<String, Object>> userSkills = diagnosisMapper.selectUserSkillTags(userId);
            for (Map<String, Object> skillRow : userSkills) {
                String tagName = skillRow.get("tag_name").toString().toLowerCase();
                String level   = skillRow.get("skill_level").toString();
                MemberSkillInfo info = new MemberSkillInfo(userId, String.valueOf(userId), level);
                result.computeIfAbsent(tagName, k -> new ArrayList<>()).add(info);
            }
        }
        return result;
    }

    /**
     * 为所需技能找到匹配的成员，先精确匹配，再用 Embedding 相似度匹配
     */
    private List<MemberSkillInfo> findMatchedMembers(String requiredSkill,
                                                      Map<String, List<MemberSkillInfo>> teamSkillMap) {
        String lowerRequired = requiredSkill.toLowerCase();

        // 精确匹配（大小写不敏感）
        if (teamSkillMap.containsKey(lowerRequired)) {
            return teamSkillMap.get(lowerRequired);
        }

        // 包含匹配（如 "Java" 匹配 "java后端"）
        for (Map.Entry<String, List<MemberSkillInfo>> entry : teamSkillMap.entrySet()) {
            if (entry.getKey().contains(lowerRequired) || lowerRequired.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Embedding 近义词匹配：计算所需技能与队伍已有技能的余弦相似度
        try {
            List<Float> requiredVec = dashScopeUtil.getEmbedding(requiredSkill);
            float bestSim = 0f;
            List<MemberSkillInfo> bestMatch = Collections.emptyList();

            for (Map.Entry<String, List<MemberSkillInfo>> entry : teamSkillMap.entrySet()) {
                List<Float> teamSkillVec = dashScopeUtil.getEmbedding(entry.getKey());
                float sim = cosineSimilarity(requiredVec, teamSkillVec);
                if (sim > bestSim) {
                    bestSim = sim;
                    bestMatch = entry.getValue();
                }
            }

            if (bestSim >= SIMILARITY_THRESHOLD) {
                log.info("[SkillGap] Embedding近义词匹配 [{}] 相似度={}", requiredSkill, bestSim);
                return bestMatch;
            }
        } catch (Exception e) {
            log.warn("[SkillGap] Embedding匹配失败，跳过近义词匹配: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private float cosineSimilarity(List<Float> a, List<Float> b) {
        if (a.size() != b.size()) return 0f;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dot   += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        if (normA == 0 || normB == 0) return 0f;
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    /**
     * 内部用：成员技能信息
     */
    private static class MemberSkillInfo {
        final Integer userId;
        final String  userName;
        final String  level;

        MemberSkillInfo(Integer userId, String userName, String level) {
            this.userId   = userId;
            this.userName = userName;
            this.level    = level;
        }
    }
}
