package com.yuki.webapp.contoller;

import com.yuki.webapp.pojo.CompetitionMember;
import com.yuki.webapp.pojo.Result;
import com.yuki.webapp.pojo.review.ApplicationAIReviewDTO;
import com.yuki.webapp.service.ApplicationReviewService;
import com.yuki.webapp.service.CompetitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
public class ApplicationReviewController {

    @Autowired
    private ApplicationReviewService applicationReviewService;

    @Autowired
    private CompetitionService competitionService;

    @PostMapping("/{applicantUserId}/ai-review")
    public Result<ApplicationAIReviewDTO> aiReview(@PathVariable Integer applicantUserId,
                                                   @RequestParam Integer competitionId) {
        ApplicationAIReviewDTO dto = applicationReviewService.review(competitionId, applicantUserId);
        return Result.success(dto);
    }

    @PostMapping("/{applicantUserId}/approve")
    public Result<Void> approve(@PathVariable Integer applicantUserId,
                                @RequestParam Integer competitionId) {
        CompetitionMember member = new CompetitionMember();
        member.setCompetitionId(competitionId);
        member.setUserId(applicantUserId);
        int affected = competitionService.joinCompetition(member);
        if (affected <= 0) {
            return Result.error("审批失败：未找到报名记录");
        }
        return Result.success();
    }
}
