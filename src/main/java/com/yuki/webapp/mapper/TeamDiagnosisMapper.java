package com.yuki.webapp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 队伍诊断数据访问层
 * 对应 TeamDiagnosisMapper.xml
 */
@Mapper
public interface TeamDiagnosisMapper {

    // ─────────────────────────────────────────────────────────────────────
    // 竞赛基本信息
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 查询竞赛基本信息（title + 5个tag）
     * @return Map包含：competition_id, title, tag1~tag5, user_id(队长)
     */
    Map<String, Object> selectCompetitionBasicInfo(@Param("competitionId") Integer competitionId);

    // ─────────────────────────────────────────────────────────────────────
    // 队伍成员信息
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 查询竞赛已录取成员列表（admission_status = true）
     * @return List<Map> 每项包含 user_id, user_name
     */
    List<Map<String, Object>> selectAdmittedMembers(@Param("competitionId") Integer competitionId);

    /**
     * 查询竞赛创建者（队长）信息
     * @return Map包含 user_id, user_name
     */
    Map<String, Object> selectCompetitionCreator(@Param("competitionId") Integer competitionId);

    // ─────────────────────────────────────────────────────────────────────
    // 4.1 技能缺口分析
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 查询某用户的全部技能标签
     * @return List<Map> 每项包含 tag_name, skill_level(BEGINNER/INTERMEDIATE/ADVANCED), confidence
     */
    List<Map<String, Object>> selectUserSkillTags(@Param("userId") Integer userId);

    /**
     * 根据标签名模糊查找标准技能标签（含 tag_aliases）
     * 用于将竞赛 tag1~tag5 映射到标准标签库
     * @return List<Map> 每项包含 tag_id, tag_name, tag_aliases
     */
    List<Map<String, Object>> selectSkillTagByName(@Param("tagName") String tagName);

    // ─────────────────────────────────────────────────────────────────────
    // 4.2 时间冲突检测
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 查询某用户的时间画像（weekly_hours + busy_level）
     * @return Map包含 weekly_hours, busy_level, available_periods
     */
    Map<String, Object> selectUserTimeProfile(@Param("userId") Integer userId);

    /**
     * 查询某用户在指定时间窗口内的不可用日期
     * @param userId 用户ID
     * @param windowStart 检查窗口开始日期（yyyy-MM-dd）
     * @param windowEnd   检查窗口结束日期（yyyy-MM-dd）
     * @return List<Map> 每项包含 start_date, end_date, reason
     */
    List<Map<String, Object>> selectUserUnavailableDates(
            @Param("userId") Integer userId,
            @Param("windowStart") String windowStart,
            @Param("windowEnd") String windowEnd
    );

    // ─────────────────────────────────────────────────────────────────────
    // 4.3 经验断层 + 角色覆盖
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 查询某用户的参赛经历列表
     * @return List<Map> 每项包含 competition_name, role, award, description
     */
    List<Map<String, Object>> selectUserCompetitionExperiences(@Param("userId") Integer userId);

    /**
     * 查询某用户画像的竞赛经验评分和技术深度评分
     * @return Map包含 score_competition, score_tech_depth, composite_score
     */
    Map<String, Object> selectUserProfileScores(@Param("userId") Integer userId);

    /**
     * 查询某用户的擅长方向列表
     * @return List<Map> 每项包含 domain, confidence
     */
    List<Map<String, Object>> selectUserDomains(@Param("userId") Integer userId);
}
