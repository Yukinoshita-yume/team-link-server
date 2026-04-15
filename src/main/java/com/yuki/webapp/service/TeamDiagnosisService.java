package com.yuki.webapp.service;

import com.yuki.webapp.pojo.TeamDiagnosisRequest;
import com.yuki.webapp.pojo.TeamDiagnosisReport;

/**
 * F4 队伍诊断服务接口
 */
public interface TeamDiagnosisService {

    /**
     * 执行队伍诊断，返回完整诊断报告
     *
     * @param request 包含 competitionId
     * @return 诊断报告
     */
    TeamDiagnosisReport diagnose(TeamDiagnosisRequest request);
}
