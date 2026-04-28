package com.yuki.webapp.contoller;

import com.yuki.webapp.pojo.CompetitionMember;
import com.yuki.webapp.pojo.Message;
import com.yuki.webapp.pojo.Result;
import com.yuki.webapp.pojo.review.ApplicationAIReviewDTO;
import com.yuki.webapp.service.ApplicationReviewService;
import com.yuki.webapp.service.CompetitionService;
import com.yuki.webapp.service.MessageService;
import com.yuki.webapp.mapper.CompetitionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
public class ApplicationReviewController {

    @Autowired
    private ApplicationReviewService applicationReviewService;

    @Autowired
    private CompetitionService competitionService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private CompetitionMapper competitionMapper;

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

        // 发送审核通过消息给申请者
        try {
            String title = competitionMapper.getTitleById(competitionId);
            Message msg = new Message();
            msg.setUserId(applicantUserId);
            msg.setCompetitionId(competitionId);
            msg.setMessageType("APPLICATION_APPROVED");
            msg.setMessageContent("恭喜！你报名的「" + (title != null ? title : "竞赛") + "」审核已通过，你已正式加入队伍。");
            messageService.createMessage(msg);
        } catch (Exception e) {
            // 消息发送失败不影响主流程
        }

        return Result.success();
    }
}
