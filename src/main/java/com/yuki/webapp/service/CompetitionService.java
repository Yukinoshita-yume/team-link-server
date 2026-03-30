package com.yuki.webapp.service;

import com.yuki.webapp.pojo.*;

import java.util.List;
import java.util.Map;

public interface CompetitionService {
    //分页查询
    PageBean selectCompetition(Integer page, Integer pageSize, String content);

    //创建竞赛
    void insertCompetition(Competition competition);

    void applyCompetition(CompetitionMember competitionMember);

    int joinCompetition(CompetitionMember competitionMember);

    List<CompetitionUser> allMembers(Integer CompetitionId);

    CompetitionUser creator(Integer competitionId);

    CompetitionDetail competitionDetail(Integer competitionId);

    boolean checkApplication(Integer userId, Integer competitionId);

    Result<Void> cancelRegistration(Integer competitionId, Integer userId);

    List<AllCompetitionsDTO> getAllCreatedCompetitions(Integer userId);

    List<AllCompetitionsDTO> getAllAppliedCompetitions(Integer userId);

    Result deleteCompetition(Integer competitionId);

    List<Competition> getAllCompetitions();
}
