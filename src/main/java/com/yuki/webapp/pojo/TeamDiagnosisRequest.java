package com.yuki.webapp.pojo;

import lombok.Data;

/**
 * 队伍诊断请求参数
 */
@Data
public class TeamDiagnosisRequest {

    /**
     * 竞赛ID（即队伍ID，competition表中的competition_id）
     */
    private Integer competitionId;
}
