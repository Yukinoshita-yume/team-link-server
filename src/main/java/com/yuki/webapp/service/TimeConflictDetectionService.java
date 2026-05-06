package com.yuki.webapp.service;

import com.yuki.webapp.mapper.TeamDiagnosisMapper;
import com.yuki.webapp.pojo.TimeConflictResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 时间冲突检测服务
 *
 * 逻辑：
 * 1. 从 user_profile 取每位成员的 weekly_hours 和 busy_level
 * 2. 求全队 weekly_hours 的最小值作为"最差重叠估算"（保守策略）
 * 3. 标记 weekly_hours < 5 或 busy_level = BUSY 的高风险成员
 * 4. 查询未来 90 天内的 user_unavailable_date，按成员分组预警
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeConflictDetectionService {

    private final TeamDiagnosisMapper diagnosisMapper;

    private static final int HIGH_RISK_HOURS_THRESHOLD = 5;

    private static final int LOOKAHEAD_DAYS = 90;

    /**
     * 检测时间冲突
     *
     * @param memberInfoList 全队成员信息列表（含 userId, userName）
     * @return 时间冲突检测结果
     */
    public TimeConflictResult detect(List<Map<String, Object>> memberInfoList) {

        TimeConflictResult result = new TimeConflictResult();
        List<TimeConflictResult.MemberTimeInfo> highRiskMembers = new ArrayList<>();
        List<TimeConflictResult.UnavailableWarning> warnings = new ArrayList<>();

        int minWeeklyHours = Integer.MAX_VALUE;

        LocalDate now = LocalDate.now();
        String windowStart = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String windowEnd   = now.plusDays(LOOKAHEAD_DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE);

        for (Map<String, Object> memberInfo : memberInfoList) {
            Integer userId   = (Integer) memberInfo.get("user_id");
            String  userName = (String) memberInfo.get("user_name");

            // 查询时间画像
            Map<String, Object> timeProfile = diagnosisMapper.selectUserTimeProfile(userId);

            int weeklyHours = 0;
            String busyLevel = "NORMAL";

            if (timeProfile != null) {
                Object wh = timeProfile.get("weekly_hours");
                Object bl = timeProfile.get("busy_level");
                Object sa = timeProfile.get("score_availability");

                if (wh != null && ((Number) wh).intValue() > 0) {
                    // weekly_hours 有实际数据，优先使用
                    weeklyHours = ((Number) wh).intValue();
                } else if (sa != null) {
                    // weekly_hours 未填，用 score_availability（0~100）反推：满分100 → 20h/周
                    weeklyHours = ((Number) sa).intValue() * 20 / 100;
                }

                if (bl != null) busyLevel = bl.toString();
            } else {
                log.warn("[TimeConflict] userId={} 无时间画像，视为 0h/周", userId);
            }

            // 累计最小值（用于估算全队重叠）
            minWeeklyHours = Math.min(minWeeklyHours, weeklyHours);

            // 判断高风险
            if (weeklyHours < HIGH_RISK_HOURS_THRESHOLD || "BUSY".equals(busyLevel)) {
                TimeConflictResult.MemberTimeInfo memberTimeInfo = new TimeConflictResult.MemberTimeInfo();
                memberTimeInfo.setUserId(userId);
                memberTimeInfo.setUserName(userName);
                memberTimeInfo.setWeeklyHours(weeklyHours);
                memberTimeInfo.setBusyLevel(busyLevel);
                highRiskMembers.add(memberTimeInfo);
            }

            // 查询不可用时间段预警
            List<Map<String, Object>> unavailableDates =
                    diagnosisMapper.selectUserUnavailableDates(userId, windowStart, windowEnd);

            for (Map<String, Object> dateRow : unavailableDates) {
                TimeConflictResult.UnavailableWarning warn = new TimeConflictResult.UnavailableWarning();
                warn.setUserId(userId);
                warn.setUserName(userName);
                warn.setStartDate(dateRow.get("start_date").toString());
                warn.setEndDate(dateRow.get("end_date").toString());
                warn.setReason(dateRow.get("reason") != null ? dateRow.get("reason").toString() : "");
                warnings.add(warn);
            }
        }

        // 如果没有任何成员有画像，重叠时间视为 0
        int overlapHours = (minWeeklyHours == Integer.MAX_VALUE) ? 0 : minWeeklyHours;

        result.setWeeklyOverlapHours(overlapHours);
        result.setHighRisk(overlapHours < HIGH_RISK_HOURS_THRESHOLD || !highRiskMembers.isEmpty());
        result.setHighRiskMembers(highRiskMembers);

        // 按时间排序预警
        warnings.sort(Comparator.comparing(TimeConflictResult.UnavailableWarning::getStartDate));
        result.setUpcomingWarnings(warnings);

        log.info("[TimeConflict] 估算重叠={}h/周, 高风险成员={}人, 预警条数={}",
                overlapHours, highRiskMembers.size(), warnings.size());

        return result;
    }
}
