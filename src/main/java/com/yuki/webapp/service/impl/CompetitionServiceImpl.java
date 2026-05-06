package com.yuki.webapp.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yuki.webapp.mapper.CompetitionMapper;
import com.yuki.webapp.mapper.MessageMapper;
import com.yuki.webapp.pojo.*;
import com.yuki.webapp.service.CompetitionSearchService;
import com.yuki.webapp.service.CompetitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompetitionServiceImpl implements CompetitionService {

    @Autowired
    private CompetitionMapper competitionMapper;

    @Autowired
    private MessageMapper messageMapper;

    // 统一使用 CompetitionSearchService，不再依赖 CompetitionIndexService
    @Autowired
    private CompetitionSearchService competitionSearchService;

    // ── 分页查询 ──────────────────────────────────────────────────────
    @Override
    public PageBean selectCompetition(Integer page, Integer pageSize, String content) {
        PageHelper.startPage(page, pageSize);
        List<CompetitionDTO> allCompetitionList = competitionMapper.selectCompetition(content);
        PageInfo<Competition> pageInfo = new PageInfo<>(allCompetitionList);

        PageBean pageBean = new PageBean();
        pageBean.setTotal(pageInfo.getTotal());
        pageBean.setRows(pageInfo.getList());
        return pageBean;
    }

    // ── 新建竞赛：写 MySQL → 同步索引
    @Override
    public void insertCompetition(Competition competition) {
        competition.setCompetitionCreatedTime(LocalDateTime.now());
        competition.setCompetitionUpdatedTime(LocalDateTime.now());
        competitionMapper.insertCompetition(competition);
        // insertCompetition 的 Mapper 需配置 useGeneratedKeys="true" keyProperty="competitionId"
        // 确保此处 competition.getCompetitionId() 已被 MyBatis 回填
        competitionSearchService.addToIndex(competition);
    }

    @Override
    public void updateCompetition(Competition competition) {
        competition.setCompetitionUpdatedTime(LocalDateTime.now());
        competitionMapper.updateCompetition(competition);
        // 从数据库重新查一次，保证索引数据完整（包含 details 等大字段）
        Competition full = competitionMapper.getById(competition.getCompetitionId());
        if (full != null) {
            competitionSearchService.addToIndex(full);
        }
    }

    @Override
    public void applyCompetition(CompetitionMember competitionMember) {
        competitionMember.setCompetitionMemberCreatedTime(LocalDateTime.now());
        competitionMember.setCompetitionMemberUpdatedTime(LocalDateTime.now());
        competitionMapper.applyCompetition(competitionMember);
    }

    @Override
    public int joinCompetition(CompetitionMember competitionMember) {
        return competitionMapper.joinCompetition(competitionMember);
    }

    @Override
    public List<CompetitionUser> allMembers(Integer competitionId) {
        return competitionMapper.selectAllMembers(competitionId);
    }

    @Override
    public CompetitionUser creator(Integer competitionId) {
        return competitionMapper.selectCreator(competitionId);
    }

    @Override
    public CompetitionDetail competitionDetail(Integer competitionId) {
        CompetitionDetail detail = competitionMapper.competitionDetail(competitionId);
        List<CompetitionUser> members = competitionMapper.getAdmittedMembers(competitionId);
        detail.setAdmittedMemberNames(members);
        return detail;
    }

    @Override
    public boolean checkApplication(Integer userId, Integer competitionId) {
        if (userId == null || competitionId == null) return false;
        return competitionMapper.checkApplication(userId, competitionId);
    }

    @Override
    @Transactional
    public Result<Void> cancelRegistration(Integer competitionId, Integer userId) {
        try {
            int affectedRows = competitionMapper.cancelRegistration(competitionId, userId);
            if (affectedRows > 0) {
                return Result.success();
            } else {
                return Result.error("取消报名失败，用户未报名该比赛");
            }
        } catch (Exception e) {
            return Result.error("取消报名时发生错误: " + e.getMessage());
        }
    }

    @Override
    public List<AllCompetitionsDTO> getAllCreatedCompetitions(Integer userId) {
        return competitionMapper.getAllCreatedCompetitions(userId);
    }

    @Override
    public List<AllCompetitionsDTO> getAllAppliedCompetitions(Integer userId) {
        return competitionMapper.getAllAppliedCompetitions(userId);
    }

    @Override
    public List<AllCompetitionsDTO> getAllRegisteredCompetitions(Integer userId) {
        return competitionMapper.getAllRegisteredCompetitions(userId);
    }

    @Override
    public int getUnreadMessageCount(Integer userId) {
        return competitionMapper.getUnreadMessageCount(userId);
    }

    @Override
    public int getPendingReviewCount(Integer userId) {
        return competitionMapper.getPendingReviewCount(userId);
    }

    @Override
    public void markAllReviewed(Integer competitionId) {
        competitionMapper.markAllReviewed(competitionId);
    }

    @Override
    public int getPendingReviewCountByCompetition(Integer competitionId) {
        return competitionMapper.getPendingReviewCountByCompetition(competitionId);
    }

    @Override
    @Transactional
    public Result deleteCompetition(Integer competitionId) {
        try {
            String title = competitionMapper.getTitleById(competitionId);
            if (title == null) {
                return Result.error("未找到对应的竞赛记录");
            }

            List<Integer> memberIds = competitionMapper.getUserIdsByCompetitionId(competitionId);
            String messageContent = "你报名的" + title + "已被解散";
            for (Integer userId : memberIds) {
                Message message = new Message();
                message.setUserId(userId);
                message.setCompetitionId(competitionId);
                message.setMessageType("TEAM_DISBANDED");
                message.setMessageContent(messageContent);
                message.setIsRead(false);
                message.setMessageCreatedTime(LocalDateTime.now());
                messageMapper.insertMessage(message);
            }

            competitionMapper.deleteMembers(competitionId);
            competitionMapper.deleteCompetition(competitionId);
            // 统一用 CompetitionSearchService 删除索引
            competitionSearchService.deleteFromIndex(competitionId);

            return Result.success();
        } catch (Exception e) {
            return Result.error("解散竞赛队伍失败: " + e.getMessage());
        }
    }

    @Override
    public List<Competition> getAllCompetitions() {
        return competitionMapper.getAllCompetitions();
    }
}