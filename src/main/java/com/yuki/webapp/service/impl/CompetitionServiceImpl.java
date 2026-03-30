package com.yuki.webapp.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yuki.webapp.mapper.CompetitionMapper;
import com.yuki.webapp.mapper.MessageMapper;
import com.yuki.webapp.pojo.*;
import com.yuki.webapp.service.CompetitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CompetitionServiceImpl implements CompetitionService {
    @Autowired
    private CompetitionMapper competitionMapper;

    @Autowired
    private MessageMapper messageMapper;

    /**
     * @param page               页码
     * @param pageSize           每页记录数
     * @param content
     * @return PageBean对象，封装总记录数和一页的数据
     */
    //分页查询
    @Override
    public PageBean selectCompetition(Integer page, Integer pageSize, String content) {
        //1.设置分页查询的参数
        PageHelper.startPage(page,pageSize);//页码，每页显示数
        //2.查询所有数据
        List<CompetitionDTO> allCompetitionList = competitionMapper.selectCompetition(content);
        //3.使用 PageInfo 封装分页结果
        PageInfo<Competition> pageInfo = new PageInfo<>(allCompetitionList);
        long total = pageInfo.getTotal();
        List<Competition> competitionList = pageInfo.getList();

        //创建PageBean对象,把总记录数和此页所有数据封装到PageBean对象
        PageBean pageBean = new PageBean();
        pageBean.setTotal(total);
        pageBean.setRows(competitionList);

        return pageBean;
    }

    @Override
    public void insertCompetition(Competition competition) {
        competition.setCompetitionCreatedTime(LocalDateTime.now());
        competition.setCompetitionUpdatedTime(LocalDateTime.now());
        competitionMapper.insertCompetition(competition);
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
        // 1. 查询竞赛基础信息
        CompetitionDetail detail = competitionMapper.competitionDetail(competitionId);

        // 2. 查询已录取成员名单
        List<CompetitionUser> members = competitionMapper.getAdmittedMembers(competitionId);

        // 3. 合并结果
        detail.setAdmittedMemberNames(members);

        return detail;
    }

    @Override
    public boolean checkApplication(Integer userId, Integer competitionId) {
        if (userId == null || competitionId == null) {
            return false;
        }
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

    // 查询一个用户创建的所有竞赛
    @Override
    public List<AllCompetitionsDTO> getAllCreatedCompetitions(Integer userId) {
        return competitionMapper.getAllCreatedCompetitions(userId);
    }

    // 查询一个用户参加的所有竞赛
    @Override
    public List<AllCompetitionsDTO> getAllAppliedCompetitions(Integer userId) {
        return competitionMapper.getAllAppliedCompetitions(userId);
    }

    @Override
    @Transactional
    public Result deleteCompetition(Integer competitionId) {
        try {
            // 1. 获取竞赛信息
            String title = competitionMapper.getTitleById(competitionId);
            if (title == null) {
                return Result.error("未找到对应的竞赛记录");
            }

            // 2. 获取所有报名成员ID
            List<Integer> memberIds = competitionMapper.getUserIdsByCompetitionId(competitionId);

            String messageContent = "你报名的" + title + "已被解散";
            // 4. 为每个成员插入消息
            for (Integer userId : memberIds) {
                Message message = new Message();
                message.setUserId(userId);
                message.setCompetitionId(competitionId);
                message.setMessageType("TEAM_DISBANDED");
                message.setMessageContent(messageContent);
                message.setIsRead(false);
                message.setMessageCreatedTime(LocalDateTime.now());
                // 插入数据库
                messageMapper.insertMessage(message);
            }

            // 5. 删除成员记录
            competitionMapper.deleteMembers(competitionId);
            // 6. 删除竞赛记录
            competitionMapper.deleteCompetition(competitionId);

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
