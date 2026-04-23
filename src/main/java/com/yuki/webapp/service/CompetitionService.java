package com.yuki.webapp.service;

import com.yuki.webapp.pojo.*;

import java.util.List;

public interface CompetitionService {

    // 分页查询
    PageBean selectCompetition(Integer page, Integer pageSize, String content);

    // 新建竞赛
    void insertCompetition(Competition competition);

    // 更新竞赛（新增，同步索引用）
    void updateCompetition(Competition competition);

    void applyCompetition(CompetitionMember competitionMember);

    int joinCompetition(CompetitionMember competitionMember);

    List<CompetitionUser> allMembers(Integer competitionId);

    CompetitionUser creator(Integer competitionId);

    CompetitionDetail competitionDetail(Integer competitionId);

    boolean checkApplication(Integer userId, Integer competitionId);

    Result<Void> cancelRegistration(Integer competitionId, Integer userId);

    List<AllCompetitionsDTO> getAllCreatedCompetitions(Integer userId);

    List<AllCompetitionsDTO> getAllAppliedCompetitions(Integer userId);

    Result deleteCompetition(Integer competitionId);

    List<Competition> getAllCompetitions();
}