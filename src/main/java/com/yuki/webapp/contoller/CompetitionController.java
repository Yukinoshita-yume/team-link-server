package com.yuki.webapp.contoller;

import com.yuki.webapp.pojo.*;
import com.yuki.webapp.service.CompetitionIndexService;
import com.yuki.webapp.service.CompetitionService;
import com.yuki.webapp.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/index")
public class CompetitionController {
    @Autowired
    private CompetitionService competitionService;

    @PostMapping("/selectPage")
    //分页查询,条件查询
    public Result selectPage(@RequestBody PageSelect pageSelect){
        Integer page= pageSelect.getPage();
        Integer pageSize= pageSelect.getPageSize();
        String content= pageSelect.getContent();
        PageBean pageBean = competitionService.selectCompetition(page,pageSize,content);
        return Result.success(pageBean);
    }

    //创建竞赛
    @PostMapping("/createCompetition")
    public Result createCompetition(@RequestBody Competition competition){
        competitionService.insertCompetition(competition);
        return Result.success();
    }

    //报名竞赛
    @PostMapping("/applyCompetition")
    public Result applyCompetition(@RequestBody CompetitionMember competitionMember){
        competitionService.applyCompetition(competitionMember);
        return Result.success();
    }

    //加入竞赛(报名成功)
    @PostMapping("/joinCompetition")
    public Result joinCompetition(@RequestBody CompetitionMember competitionMember){
        int result = competitionService.joinCompetition(competitionMember);
        return Result.success(result);
    }

    //查询一个竞赛的所有参与者
    @GetMapping("/allMembers")
    public Result allMembers(@RequestParam("competitionId") Integer competitionId){
        List<CompetitionUser> memberNames = competitionService.allMembers(competitionId);
        return Result.success(memberNames);
    }

    //查询一个竞赛的创建者
    @GetMapping("/creator")
    public Result creator(@RequestParam("competitionId") Integer competitionId){
        CompetitionUser creatorName = competitionService.creator(competitionId);
        return Result.success(creatorName);
    }

    //查询竞赛的详细信息
    @GetMapping("/competitionDetail")
    public Result competitionDetail(@RequestParam("competitionId") Integer competitionId){
        CompetitionDetail competitionDetail = competitionService.competitionDetail(competitionId);
        return Result.success(competitionDetail);
    }

    //查询用户是否报名
    @GetMapping("/checkApplication")
    public Result<Boolean> checkApplication(
            @RequestParam Integer userId,
            @RequestParam Integer competitionId) {
        boolean hasApplied = competitionService.checkApplication(userId, competitionId);
        return Result.success(hasApplied);
    }

    //取消报名
    @DeleteMapping("/cancelRegistration")
    public Result<Void> cancelRegistration(
            @RequestParam Integer competitionId,
            @RequestParam Integer userId) {
        return competitionService.cancelRegistration(competitionId, userId);
    }

    // 查询一个用户创建的所有竞赛
    @GetMapping("/allCreatedCompetitions")
    public Result allCreatedCompetitions(@RequestParam("userId") Integer userId){
        List<AllCompetitionsDTO> allCreatedCompetitions = competitionService.getAllCreatedCompetitions(userId);
        return Result.success(allCreatedCompetitions);
    }

    // 查询一个用户参加的所有竞赛（已录取，admission_status=1）
    @GetMapping("/allAppliedCompetitions")
    public Result allAppliedCompetitions(@RequestParam("userId") Integer userId){
        List<AllCompetitionsDTO> allAppliedCompetitions = competitionService.getAllAppliedCompetitions(userId);
        return Result.success(allAppliedCompetitions);
    }

    // 查询一个用户报名但尚未审核通过的竞赛（待审核，admission_status=0）
    @GetMapping("/allRegisteredCompetitions")
    public Result allRegisteredCompetitions(@RequestParam("userId") Integer userId){
        List<AllCompetitionsDTO> list = competitionService.getAllRegisteredCompetitions(userId);
        return Result.success(list);
    }

    // 查询用户未读消息数 + 待审核报名数（用于徽章提示）
    @GetMapping("/notificationCounts")
    public Result notificationCounts(@RequestParam("userId") Integer userId){
        int unreadMsg = competitionService.getUnreadMessageCount(userId);
        int pendingReview = competitionService.getPendingReviewCount(userId);
        java.util.Map<String, Integer> data = new java.util.HashMap<>();
        data.put("unreadMessage", unreadMsg);
        data.put("pendingReview", pendingReview);
        return Result.success(data);
    }

    // 队长打开审核页时调用，将该竞赛所有待审核申请标记为"已查看"，清除红点
    @PutMapping("/markReviewed")
    public Result markReviewed(@RequestParam("competitionId") Integer competitionId){
        competitionService.markAllReviewed(competitionId);
        return Result.success();
    }

    // 解散队伍
    @DeleteMapping("/deleteCompetitioin")
    public Result deleteCompetitioin(@RequestParam Integer competitionId){
        return competitionService.deleteCompetition(competitionId);
    }

    @Autowired
    private CompetitionIndexService competitionIndexService;

    @PostMapping("/syncIndex")
    public Result syncIndex() {
        List<Competition> all = competitionService.getAllCompetitions();
        LocalDateTime now = LocalDateTime.now();

        for (Competition c : all) {
            if (c.getDeadline() == null || c.getDeadline().isAfter(now)) {
                competitionIndexService.indexCompetition(c);
            }
        }
        return Result.success("同步完成，共 " + all.size() + " 条");
    }

    @GetMapping("/pendingReviewCount")
    public Result pendingReviewCount(@RequestParam("competitionId") Integer competitionId) {
        int count = competitionService.getPendingReviewCountByCompetition(competitionId);
        return Result.success(count);
    }
}