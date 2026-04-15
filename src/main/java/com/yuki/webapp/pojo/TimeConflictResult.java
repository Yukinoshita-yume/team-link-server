package com.yuki.webapp.pojo;

import lombok.Data;

import java.util.List;

/**
 * 4.2 时间冲突检测结果
 */
@Data
public class TimeConflictResult {

    /** 全队每周重叠可用小时数 */
    private Integer weeklyOverlapHours;

    /** 是否为高风险（重叠时间 < 5h/周） */
    private boolean highRisk;

    /** 高风险成员列表（个人可用时间极少或与队伍时间严重不重叠） */
    private List<MemberTimeInfo> highRiskMembers;

    /** 即将到来的不可用时间段预警（考试周、假期等） */
    private List<UnavailableWarning> upcomingWarnings;

    @Data
    public static class MemberTimeInfo {
        private Integer userId;
        private String userName;
        /** 每周可用小时数 */
        private Integer weeklyHours;
        /** 短期可用等级：FREE / NORMAL / BUSY */
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
