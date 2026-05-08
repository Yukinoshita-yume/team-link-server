package com.yuki.webapp.mapper;

import com.yuki.webapp.pojo.*;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CompetitionMapper {

    // 条件查询
    List<CompetitionDTO> selectCompetition(String content);

    // 创办竞赛
    @Insert("insert into competition (user_id, title, tag1, tag2, tag3, tag4, tag5, competition_details, max_participants, school_requirements, deadline, competition_created_time, competition_updated_time) " +
            "values (#{userId}, #{title}, #{tag1}, #{tag2}, #{tag3}, #{tag4}, #{tag5}, #{competitionDetails}, #{maxParticipants}, #{schoolRequirements}, #{deadline}, #{competitionCreatedTime}, #{competitionUpdatedTime})")
    @Options(useGeneratedKeys = true, keyProperty = "competitionId")
    void insertCompetition(Competition competition);

    // 更新竞赛
    @Update("update competition set title=#{title}, tag1=#{tag1}, tag2=#{tag2}, tag3=#{tag3}, tag4=#{tag4}, tag5=#{tag5}, " +
            "competition_details=#{competitionDetails}, max_participants=#{maxParticipants}, " +
            "school_requirements=#{schoolRequirements}, deadline=#{deadline}, " +
            "competition_updated_time=#{competitionUpdatedTime} " +
            "where competition_id=#{competitionId}")
    void updateCompetition(Competition competition);

    // 按 ID 查询单条竞赛完整信息
    @Select("select * from competition where competition_id = #{competitionId}")
    Competition getById(@Param("competitionId") Integer competitionId);

    @Select("select * from competition")
    List<Competition> getAllCompetitions();

    // 报名竞赛
    @Insert("insert into competition_member (competition_id, user_id, is_reviewed) values (#{competitionId}, #{userId}, false)")
    void applyCompetition(CompetitionMember competitionMember);

    // 加入竞赛（录取）
    @Update("update competition_member set admission_status = 1 where competition_id = #{competitionId} and user_id = #{userId}")
    int joinCompetition(CompetitionMember competitionMember);

    // 查询一个竞赛的所有已录取参与者
    @Select("select u.user_id, u.user_name from user u, cm.admission_status " +
            "join competition_member cm on u.user_id = cm.user_id " +
            "where cm.competition_id = #{competitionId} and cm.admission_status = 1")
    List<CompetitionUser> selectAllMembers(@Param("competitionId") Integer competitionId);

    //查询一个竞赛的最大参与人数
    @Select("select max_participants from competition where competition_id = #{competitionId}")
    Integer selectMaxParticipants(@Param("competitionId") Integer competitionId);

    // 查询一个竞赛的所有报名者（含录取状态）
    @Select("select u.user_id, u.user_name, cm.admission_status from user u " +
            "join competition_member cm on u.user_id = cm.user_id " +
            "where cm.competition_id = #{competitionId}")
    List<CompetitionUser> selectAllApplicants(@Param("competitionId") Integer competitionId);

    // 查询一个竞赛的创建者
    @Select("select u.user_id, u.user_name from user u " +
            "join competition c on u.user_id = c.user_id " +
            "where c.competition_id = #{competitionId}")
    CompetitionUser selectCreator(@Param("competitionId") Integer competitionId);

    // 查询竞赛详细信息
    CompetitionDetail competitionDetail(@Param("competitionId") Integer competitionId);
    List<CompetitionUser> getAdmittedMembers(@Param("competitionId") Integer competitionId);

    // 查询用户是否报名
    @Select("select count(*) > 0 from competition_member where user_id = #{userId} and competition_id = #{competitionId}")
    boolean checkApplication(@Param("userId") Integer userId, @Param("competitionId") Integer competitionId);

    // 取消报名
    @Delete("delete from competition_member where competition_id = #{competitionId} and user_id = #{userId}")
    int cancelRegistration(@Param("competitionId") Integer competitionId, @Param("userId") Integer userId);

    // 查询一个用户创建的所有竞赛
    @Select("select competition_id, title from competition where user_id = #{userId}")
    List<AllCompetitionsDTO> getAllCreatedCompetitions(@Param("userId") Integer userId);

    // 查询一个用户参加的所有竞赛（admission_status=1，已录取）
    @Select("select c.competition_id, c.title from competition c " +
            "join competition_member cm on c.competition_id = cm.competition_id " +
            "where cm.user_id = #{userId} and cm.admission_status = 1")
    List<AllCompetitionsDTO> getAllAppliedCompetitions(@Param("userId") Integer userId);

    // 查询一个用户报名但尚未审核通过的所有竞赛（admission_status=0，待审核）
    @Select("select c.competition_id, c.title from competition c " +
            "join competition_member cm on c.competition_id = cm.competition_id " +
            "where cm.user_id = #{userId} and cm.admission_status = 0")
    List<AllCompetitionsDTO> getAllRegisteredCompetitions(@Param("userId") Integer userId);

    // 查询用户未读消息数
    @Select("select count(*) from message where user_id = #{userId} and is_read = false\n" +
            "and (message_type is null or message_type != 'DIRECT')")
    int getUnreadMessageCount(@Param("userId") Integer userId);

    // 查询用户创建的竞赛中待审核且队长未查看的报名数
    @Select("select count(*) from competition_member cm " +
            "join competition c on cm.competition_id = c.competition_id " +
            "where c.user_id = #{userId} and cm.admission_status = 0 and (cm.is_reviewed = false or cm.is_reviewed is null)")
    int getPendingReviewCount(@Param("userId") Integer userId);

    // 将某竞赛的所有待审核申请标记为"已查看"
    @Update("update competition_member set is_reviewed = true " +
            "where competition_id = #{competitionId} and admission_status = 0")
    void markAllReviewed(@Param("competitionId") Integer competitionId);

    @Delete("delete from competition_member where competition_id = #{competitionId}")
    void deleteMembers(Integer competitionId);

    @Delete("delete from competition where competition_id = #{competitionId}")
    void deleteCompetition(Integer competitionId);

    @Select("select user_id from competition_member where competition_id = #{competitionId}")
    List<Integer> getUserIdsByCompetitionId(Integer competitionId);

    @Select("select title from competition where competition_id = #{competitionId}")
    String getTitleById(Integer competitionId);

    @Select("<script>" +
            "select competition_id from competition " +
            "where competition_id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Integer> findExistingIds(@Param("ids") List<Integer> ids);

    @Select("select count(*) from competition_member " +
            "where competition_id = #{competitionId} " +
            "and admission_status = 0 " +
            "and (is_reviewed = false or is_reviewed is null)")
    int getPendingReviewCountByCompetition(@Param("competitionId") Integer competitionId);
}