package com.yuki.webapp.service.impl;

import com.yuki.webapp.mapper.TeamDiagnosisMapper;
import com.yuki.webapp.pojo.TeamDiagnosisRequest;
import com.yuki.webapp.pojo.*;
import com.yuki.webapp.service.*;
import com.yuki.webapp.utils.DashScopeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * F4 队伍诊断服务实现
 *
 * 编排顺序：
 * 1. 查竞赛信息 + 全队成员
 * 2. 并行执行三个子诊断（此处为顺序调用，可后续改为 CompletableFuture）
 * 3. 汇总评分，计算总分和风险等级
 * 4. 调用 LLM 生成自然语言优化建议
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamDiagnosisServiceImpl implements TeamDiagnosisService {

    private final TeamDiagnosisMapper       diagnosisMapper;
    private final SkillGapAnalysisService   skillGapService;
    private final TimeConflictDetectionService timeConflictService;
    private final ExperienceRoleCoverageService experienceRoleService;
    private final DashScopeUtil             dashScopeUtil;

    // LLM 建议生成的 System Prompt
    private static final String AI_SUGGESTION_SYSTEM_PROMPT =
            "你是一位专业的竞赛组队顾问。根据队伍诊断结果，给出3-5条具体可执行的优化建议。" +
            "要求：简洁直接，每条建议不超过50字，避免空话，聚焦最高优先级的问题。" +
            "输出格式：每条建议单独一行，用数字序号开头，如「1. 建议内容」。";

    @Override
    public TeamDiagnosisReport diagnose(TeamDiagnosisRequest request) {
        long startTime = System.currentTimeMillis();
        Integer competitionId = request.getCompetitionId();

        // Step 1: 查竞赛基本信息
        Map<String, Object> competitionInfo = diagnosisMapper.selectCompetitionBasicInfo(competitionId);
        if (competitionInfo == null) {
            throw new RuntimeException("竞赛不存在: competitionId=" + competitionId);
        }
        String title = competitionInfo.get("title").toString();
        log.info("[Diagnosis] 开始诊断竞赛「{}」(id={})", title, competitionId);

        // Step 2: 查全队成员（队长 + 已录取成员）
        List<Map<String, Object>> allMembers = buildAllMemberList(competitionId);
        List<Integer> allMemberIds = allMembers.stream()
                .map(m -> (Integer) m.get("user_id"))
                .toList();
        log.info("[Diagnosis] 全队人数: {}", allMembers.size());

        // Step 3: 执行三个子诊断
        SkillGapResult skillGap = skillGapService.analyze(competitionInfo, allMemberIds);
        TimeConflictResult timeConflict = timeConflictService.detect(allMembers);
        ExperienceRoleResult experienceRole = experienceRoleService.analyze(allMembers, competitionInfo);

        // Step 4: 计算诊断总分
        int totalScore = calculateTotalScore(skillGap, timeConflict, experienceRole);
        String riskLevel = calcRiskLevel(totalScore);

        // Step 5: LLM 生成综合建议
        String aiSuggestion = generateAiSuggestion(title, skillGap, timeConflict, experienceRole);

        // Step 6: 组装报告
        TeamDiagnosisReport report = new TeamDiagnosisReport();
        report.setCompetitionId(competitionId);
        report.setCompetitionTitle(title);
        report.setTotalScore(totalScore);
        report.setRiskLevel(riskLevel);
        report.setSkillGap(skillGap);
        report.setTimeConflict(timeConflict);
        report.setExperienceRole(experienceRole);
        report.setAiSuggestion(aiSuggestion);
        report.setLatencyMs(System.currentTimeMillis() - startTime);

        log.info("[Diagnosis] 诊断完成，总分={}, 风险={}, 耗时={}ms",
                totalScore, riskLevel, report.getLatencyMs());

        return report;
    }

    // ─────────────────────────────────────────────────────────────────────
    // 私有方法
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 构建全队成员列表（队长 + 已录取成员，去重）
     */
    private List<Map<String, Object>> buildAllMemberList(Integer competitionId) {
        List<Map<String, Object>> allMembers = new ArrayList<>();

        Map<String, Object> creator = diagnosisMapper.selectCompetitionCreator(competitionId);
        if (creator != null) allMembers.add(creator);

        List<Map<String, Object>> admitted = diagnosisMapper.selectAdmittedMembers(competitionId);
        for (Map<String, Object> member : admitted) {
            Integer uid = (Integer) member.get("user_id");
            // 去重：避免队长同时也在 competition_member 中
            boolean alreadyAdded = allMembers.stream()
                    .anyMatch(m -> uid.equals(m.get("user_id")));
            if (!alreadyAdded) allMembers.add(member);
        }
        return allMembers;
    }

    /**
     * 根据三个子诊断结果计算诊断总分（0-100）
     *
     * 扣分规则：
     * - 每个严重技能缺口 -15 分
     * - 每个一般技能缺口 -8 分
     * - 每个轻微技能缺口 -3 分
     * - 时间高风险 -15 分
     * - 每个高风险成员额外 -5 分（最多扣 15 分）
     * - 每个 MISSING 角色 -10 分
     * - 每个 WEAK 角色 -5 分
     * - 无同类经历 -8 分
     * - 无领导经历 -5 分
     */
    private int calculateTotalScore(SkillGapResult skillGap,
                                    TimeConflictResult timeConflict,
                                    ExperienceRoleResult experienceRole) {
        int score = 100;

        // 技能缺口扣分
        score -= skillGap.getCriticalGaps().size() * 15;
        score -= skillGap.getModerateGaps().size() * 8;
        score -= skillGap.getMinorGaps().size() * 3;

        // 时间冲突扣分
        if (timeConflict.isHighRisk()) score -= 15;
        int memberRiskDeduction = Math.min(timeConflict.getHighRiskMembers().size() * 5, 15);
        score -= memberRiskDeduction;

        // 角色覆盖扣分
        for (ExperienceRoleResult.RoleCoverage rc : experienceRole.getRoleCoverages()) {
            if ("MISSING".equals(rc.getStatus())) score -= 10;
            else if ("WEAK".equals(rc.getStatus())) score -= 5;
        }

        // 经验断层扣分
        if (!experienceRole.isHasSimilarTypeExperience()) score -= 8;
        if (!experienceRole.isHasLeaderExperience())       score -= 5;

        return Math.max(0, score);
    }

    /**
     * 根据总分确定风险等级
     */
    private String calcRiskLevel(int totalScore) {
        if (totalScore >= 75) return "LOW";
        if (totalScore >= 50) return "MEDIUM";
        return "HIGH";
    }

    /**
     * 调用 LLM 生成自然语言优化建议
     * 温度设为 0.3，保证建议稳定性（需求文档 F4.4 要求）
     */
    private String generateAiSuggestion(String competitionTitle,
                                         SkillGapResult skillGap,
                                         TimeConflictResult timeConflict,
                                         ExperienceRoleResult experienceRole) {
        try {
            StringBuilder context = new StringBuilder();
            context.append("竞赛名称：").append(competitionTitle).append("\n\n");

            // 技能缺口摘要
            if (!skillGap.getCriticalGaps().isEmpty()) {
                context.append("【严重技能缺口】");
                skillGap.getCriticalGaps().forEach(g ->
                        context.append(g.getSkillName()).append("、"));
                context.append("\n");
            }
            if (!skillGap.getModerateGaps().isEmpty()) {
                context.append("【一般技能缺口】");
                skillGap.getModerateGaps().forEach(g ->
                        context.append(g.getSkillName()).append("、"));
                context.append("\n");
            }

            // 时间风险摘要
            if (timeConflict.isHighRisk()) {
                context.append("【时间风险】全队每周重叠可用时间仅 ")
                       .append(timeConflict.getWeeklyOverlapHours())
                       .append(" 小时，高于风险阈值 5h，");
                if (!timeConflict.getHighRiskMembers().isEmpty()) {
                    context.append("高风险成员：");
                    timeConflict.getHighRiskMembers().forEach(m ->
                            context.append(m.getUserName()).append("(").append(m.getBusyLevel()).append(")、"));
                }
                context.append("\n");
            }

            // 经验断层摘要
            context.append("【经验分析】").append(experienceRole.getExperienceGapDescription()).append("\n");

            // 角色覆盖摘要
            boolean hasMissingRole = experienceRole.getRoleCoverages().stream()
                    .anyMatch(r -> "MISSING".equals(r.getStatus()));
            if (hasMissingRole) {
                context.append("【角色缺失】");
                experienceRole.getRoleCoverages().stream()
                        .filter(r -> "MISSING".equals(r.getStatus()))
                        .forEach(r -> context.append(r.getRoleName()).append("、"));
                context.append("\n");
            }

            return dashScopeUtil.chat(AI_SUGGESTION_SYSTEM_PROMPT, context.toString(), 0.3);

        } catch (Exception e) {
            log.error("[Diagnosis] LLM建议生成失败，使用规则兜底: {}", e.getMessage());
            return buildFallbackSuggestion(skillGap, timeConflict, experienceRole);
        }
    }

    /**
     * LLM 不可用时的规则引擎兜底建议
     */
    private String buildFallbackSuggestion(SkillGapResult skillGap,
                                            TimeConflictResult timeConflict,
                                            ExperienceRoleResult experienceRole) {
        StringBuilder sb = new StringBuilder();
        int idx = 1;

        if (!skillGap.getCriticalGaps().isEmpty()) {
            sb.append(idx++).append(". 立即招募具备 [")
              .append(skillGap.getCriticalGaps().get(0).getSkillName())
              .append("] 技能的成员，这是当前最紧迫的缺口。\n");
        }
        if (timeConflict.isHighRisk()) {
            sb.append(idx++).append(". 全队时间严重不足，建议召开时间协调会，确认每人每周至少投入 5 小时。\n");
        }
        if (!experienceRole.isHasLeaderExperience()) {
            sb.append(idx++).append(". 建议明确队长角色，统筹进度安排和任务分工。\n");
        }
        int finalIdx = idx;
        experienceRole.getRoleCoverages().stream()
                .filter(r -> "MISSING".equals(r.getStatus()))
                .findFirst()
                .ifPresent(r -> sb.append(finalIdx).append(". [").append(r.getRoleName()).append("] 角色缺失，建议招募或由现有成员兼顾。\n"));

        return sb.length() > 0 ? sb.toString() : "队伍状态良好，保持现有节奏，注意赛前充分准备答辩材料。";
    }
}
