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

    // 查询一个用户参加的所有竞赛
    @GetMapping("/allAppliedCompetitions")
    public Result allAppliedCompetitions(@RequestParam("userId") Integer userId){
        List<AllCompetitionsDTO> allAppliedCompetitions = competitionService.getAllAppliedCompetitions(userId);
        return Result.success(allAppliedCompetitions);
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
}