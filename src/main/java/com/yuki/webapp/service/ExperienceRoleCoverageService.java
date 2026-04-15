package com.yuki.webapp.service;

import com.yuki.webapp.mapper.TeamDiagnosisMapper;
import com.yuki.webapp.pojo.ExperienceRoleResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 4.3 经验断层 + 角色覆盖分析服务
 *
 * 逻辑：
 * 1. 遍历全队成员的 user_competition_experience，统计经验场次
 * 2. 检查是否有同类型赛事经历、是否有人担任过队长/技术负责人
 * 3. 查询 user_profile.score_competition 均值，与基准分（60分）对比
 * 4. 根据成员的 user_domain 推断角色覆盖情况
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceRoleCoverageService {

    private final TeamDiagnosisMapper diagnosisMapper;

    /**
     * 历史获奖队伍竞赛经验评分基准（参考值）
     * 实际项目可从历史数据中统计，此处使用经验值 60
     */
    private static final double BASELINE_COMPETITION_SCORE = 60.0;

    /**
     * 必要角色列表及其对应的 domain 关键词
     * key=角色名, value=domain枚举值（含）
     */
    private static final Map<String, List<String>> ROLE_DOMAIN_MAP = new LinkedHashMap<>();

    static {
        ROLE_DOMAIN_MAP.put("技术开发", Arrays.asList("BACKEND", "FRONTEND", "FULLSTACK", "ALGORITHM", "AI_ML", "EMBEDDED_HARDWARE"));
        ROLE_DOMAIN_MAP.put("产品/策划", Arrays.asList("PRODUCT_DESIGN"));
        ROLE_DOMAIN_MAP.put("UI/视觉设计", Arrays.asList("PRODUCT_DESIGN"));
        ROLE_DOMAIN_MAP.put("数据/AI", Arrays.asList("AI_ML", "DATA_ANALYSIS"));
        ROLE_DOMAIN_MAP.put("答辩/表达", Collections.emptyList()); // 通过经历中的"答辩"关键词判断
    }

    /**
     * 角色判定：成员数 >= 2 视为覆盖良好，=1 视为薄弱，=0 视为缺失
     */
    private static final int WEAK_THRESHOLD = 1;

    /**
     * 分析经验断层 + 角色覆盖
     *
     * @param memberInfoList  全队成员信息（含 userId, userName）
     * @param competitionInfo 竞赛信息（含 title, tag1~tag5）
     * @return 分析结果
     */
    public ExperienceRoleResult analyze(List<Map<String, Object>> memberInfoList,
                                        Map<String, Object> competitionInfo) {

        ExperienceRoleResult result = new ExperienceRoleResult();

        int totalExpCount          = 0;
        boolean hasSimilarType     = false;
        boolean hasLeader          = false;
        double  totalCompScore     = 0;
        int     scoredMemberCount  = 0;

        // 收集每位成员的 domain 列表
        // Map<角色名 -> List<userName>>
        Map<String, List<String>> roleCoverageMap = new LinkedHashMap<>();
        for (String role : ROLE_DOMAIN_MAP.keySet()) {
            roleCoverageMap.put(role, new ArrayList<>());
        }

        // 提取竞赛所需技能关键词，用于判断"是否有同类型经历"
        String competitionTitle = competitionInfo.get("title") != null
                ? competitionInfo.get("title").toString() : "";

        for (Map<String, Object> memberInfo : memberInfoList) {
            Integer userId   = (Integer) memberInfo.get("user_id");
            String  userName = (String) memberInfo.get("user_name");

            // ── 经验统计 ──────────────────────────────────────
            List<Map<String, Object>> experiences = diagnosisMapper.selectUserCompetitionExperiences(userId);
            totalExpCount += experiences.size();

            for (Map<String, Object> exp : experiences) {
                String role  = exp.get("role") != null ? exp.get("role").toString() : "";
                String name  = exp.get("competition_name") != null ? exp.get("competition_name").toString() : "";

                // 判断是否有同类型赛事经历（名称关键词重叠）
                if (!hasSimilarType && isSimilarCompetition(name, competitionTitle)) {
                    hasSimilarType = true;
                }

                // 判断是否有领导角色经历
                if (!hasLeader && isLeaderRole(role)) {
                    hasLeader = true;
                }

                // 答辩/表达角色特殊处理
                if (role.contains("答辩") || role.contains("演讲") || role.contains("presenter")) {
                    roleCoverageMap.get("答辩/表达").add(userName);
                }
            }

            // ── 画像评分 ──────────────────────────────────────
            Map<String, Object> scores = diagnosisMapper.selectUserProfileScores(userId);
            if (scores != null && scores.get("score_competition") != null) {
                totalCompScore += ((Number) scores.get("score_competition")).doubleValue();
                scoredMemberCount++;
            }

            // ── 擅长方向 → 角色覆盖 ──────────────────────────
            List<Map<String, Object>> domains = diagnosisMapper.selectUserDomains(userId);
            Set<String> userDomainSet = new HashSet<>();
            for (Map<String, Object> d : domains) {
                userDomainSet.add(d.get("domain").toString());
            }

            for (Map.Entry<String, List<String>> entry : ROLE_DOMAIN_MAP.entrySet()) {
                String roleName = entry.getKey();
                List<String> requiredDomains = entry.getValue();
                if (requiredDomains.isEmpty()) continue; // 答辩角色单独处理
                for (String domain : requiredDomains) {
                    if (userDomainSet.contains(domain)) {
                        if (!roleCoverageMap.get(roleName).contains(userName)) {
                            roleCoverageMap.get(roleName).add(userName);
                        }
                        break;
                    }
                }
            }
        }

        // ── 汇总评分 ──────────────────────────────────────────
        double avgScore = (scoredMemberCount > 0) ? totalCompScore / scoredMemberCount : 0;

        // ── 经验断层描述 ──────────────────────────────────────
        String expDesc = buildExperienceGapDescription(avgScore, hasSimilarType, hasLeader, totalExpCount);

        // ── 构建角色覆盖列表 ──────────────────────────────────
        List<ExperienceRoleResult.RoleCoverage> coverages = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : roleCoverageMap.entrySet()) {
            ExperienceRoleResult.RoleCoverage rc = new ExperienceRoleResult.RoleCoverage();
            rc.setRoleName(entry.getKey());
            rc.setCoveredByMembers(entry.getValue());

            int count = entry.getValue().size();
            if (count == 0) {
                rc.setStatus("MISSING");
                rc.setRemark("该角色完全缺失，建议招募");
            } else if (count <= WEAK_THRESHOLD) {
                rc.setStatus("WEAK");
                rc.setRemark("仅" + count + "人覆盖，存在单点风险");
            } else {
                rc.setStatus("COVERED");
                rc.setRemark(null);
            }
            coverages.add(rc);
        }

        result.setTotalExperienceCount(totalExpCount);
        result.setHasSimilarTypeExperience(hasSimilarType);
        result.setHasLeaderExperience(hasLeader);
        result.setAvgCompetitionScore(Math.round(avgScore * 10.0) / 10.0);
        result.setExperienceGapDescription(expDesc);
        result.setRoleCoverages(coverages);

        log.info("[ExperienceRole] 总经历={}场, 均分={}, 有同类经历={}, 有领导经历={}",
                totalExpCount, avgScore, hasSimilarType, hasLeader);

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────
    // 私有方法
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 简单关键词匹配判断是否为同类型竞赛
     */
    private boolean isSimilarCompetition(String expName, String targetTitle) {
        if (expName.isBlank() || targetTitle.isBlank()) return false;
        // 提取目标竞赛标题的前4个字符作为关键词（粗粒度匹配）
        String keyword = targetTitle.length() > 4 ? targetTitle.substring(0, 4) : targetTitle;
        return expName.contains(keyword);
    }

    /**
     * 判断是否为领导角色
     */
    private boolean isLeaderRole(String role) {
        if (role == null || role.isBlank()) return false;
        String lower = role.toLowerCase();
        return lower.contains("队长") || lower.contains("负责人") || lower.contains("leader")
                || lower.contains("captain") || lower.contains("技术总监");
    }

    /**
     * 生成经验断层自然语言描述
     */
    private String buildExperienceGapDescription(double avgScore, boolean hasSimilarType,
                                                   boolean hasLeader, int totalExp) {
        StringBuilder sb = new StringBuilder();
        if (totalExp == 0) {
            sb.append("队伍整体竞赛经验不足，所有成员均无参赛记录。");
        } else {
            sb.append("队伍累计参赛经历 ").append(totalExp).append(" 场。");
        }

        if (avgScore < BASELINE_COMPETITION_SCORE) {
            sb.append("竞赛经验评分均值（")
              .append(String.format("%.1f", avgScore))
              .append("）低于历史获奖基准（")
              .append((int) BASELINE_COMPETITION_SCORE)
              .append("分），存在明显经验断层。");
        } else {
            sb.append("竞赛经验评分均值（")
              .append(String.format("%.1f", avgScore))
              .append("）达到历史获奖基准，整体经验良好。");
        }

        if (!hasSimilarType) {
            sb.append("⚠️ 无人参加过同类型竞赛，对赛制和评分规则可能不熟悉。");
        }
        if (!hasLeader) {
            sb.append("⚠️ 无人担任过队长或技术负责人，团队管理能力存在风险。");
        }

        return sb.toString();
    }
}
