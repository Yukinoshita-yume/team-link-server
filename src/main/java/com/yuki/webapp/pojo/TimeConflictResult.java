package com.yuki.webapp.pojo;

import lombok.Data;

import java.util.List;

/**
 * 时间冲突检测结果
 */
@Data
public class TimeConflictResult {

    private Integer weeklyOverlapHours;

    private boolean highRisk;

    private List<MemberTimeInfo> highRiskMembers;

    private List<UnavailableWarning> upcomingWarnings;

    @Data
    public static class MemberTimeInfo {
        private Integer userId;
        private String userName;
        private Integer weeklyHours;
        private String busyLevel;
    }

    @Data
    public static class UnavailableWarning {
        private Integer userId;
        private String userName;
        private String startDate;
        private String endDate;
        private String reason;
    }
}
