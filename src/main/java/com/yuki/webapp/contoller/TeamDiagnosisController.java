package com.yuki.webapp.contoller;

import com.yuki.webapp.pojo.TeamDiagnosisRequest;
import com.yuki.webapp.pojo.TeamDiagnosisReport;
import com.yuki.webapp.service.TeamDiagnosisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * F4 队伍诊断 Controller
 *
 * 接口：POST /api/diagnosis/team
 * 权限：需登录（队长本人调用）
 * 说明：队长传入 competitionId，系统对该竞赛队伍执行全量诊断
 */
@Slf4j
@RestController
@RequestMapping("/api/diagnosis")
@RequiredArgsConstructor
public class TeamDiagnosisController {

    private final TeamDiagnosisService teamDiagnosisService;

    /**
     * 队伍诊断接口
     *
     * 请求示例：
     * POST /api/diagnosis/team
     * {
     *   "competitionId": 5
     * }
     *
     * 响应：TeamDiagnosisReport（含总分、风险等级、三个子诊断结果、AI建议）
     */
    @PostMapping("/team")
    public ResponseEntity<?> diagnoseTeam(@RequestBody TeamDiagnosisRequest request) {
        log.info("[Controller] 收到队伍诊断请求, competitionId={}", request.getCompetitionId());

        if (request.getCompetitionId() == null) {
            return ResponseEntity.badRequest().body("competitionId 不能为空");
        }

        try {
            TeamDiagnosisReport report = teamDiagnosisService.diagnose(request);
            return ResponseEntity.ok(report);
        } catch (RuntimeException e) {
            log.error("[Controller] 诊断失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
