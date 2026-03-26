package com.yuki.webapp.mapper;

import com.yuki.webapp.pojo.*;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface CompetitionMapper {
    //条件查询
    List<CompetitionDTO> selectCompetition(String content);

    //创办竞赛
    @Insert("insert into competition (user_id, title, tag1, tag2, tag3 ,tag4 ,tag5, competition_details, max_participants, school_requirements, deadline, competition_created_time, competition_updated_time) " +
            "values (#{userId}, #{title}, #{tag1}, #{tag2},#{tag3},#{tag4},#{tag5} ,#{competitionDetails}, #{maxParticipants}, #{schoolRequirements}, #{deadline}, #{competitionCreatedTime}, #{competitionUpdatedTime})")
    @Options(useGeneratedKeys = true, keyProperty = "competitionId")//设置competitionId为自增
    void insertCompetition(Competition competition);

    //报名竞赛
    @Insert("insert into competition_member (competition_id,user_id)"+
    "values (#{competitionId},#{userId})")
    void applyCompetition(CompetitionMember competitionMember);

    //加入竞赛(报名成功)
    @Update("update competition_member set admission_status = 1 " +
            "where competition_id = #{competitionId} and user_id = #{userId}")
    int joinCompetition(CompetitionMember competitionMember);

    //查询一个竞赛的所有参与者
    @Select("select u.user_id, u.user_name from user u " +
            "join competition_member cm on u.user_id = cm.user_id " +
            "where cm.competition_id = #{competitionId} " +
            "and cm.admission_status = 1")
    List<CompetitionUser> selectAllMembers(@Param("competitionId") Integer competitionId);

    //查询一个竞赛的创建者
    @Select("select u.user_id, u.user_name from user u " +
            "join competition c on u.user_id = c.user_id " +
            "where c.competition_id = #{competitionId}")
    CompetitionUser selectCreator(@Param("competitionId") Integer competitionId);

    //查询竞赛详细信息
    CompetitionDetail competitionDetail(@Param("competitionId") Integer competitionId);
    List<CompetitionUser> getAdmittedMembers(@Param("competitionId") Integer competitionId);

    //查询用户是否报名
    @Select("select count(*)>0 from competition_member " +
    "where user_id=#{userId} and competition_id = #{competitionId}")
    boolean checkApplication(@Param("userId") Integer userId, @Param("competitionId") Integer competitionId);

    //取消报名
    @Delete("delete from competition_member where competition_id = #{competitionId} and user_id = #{userId}")
    int cancelRegistration(@Param("competitionId") Integer competitionId, @Param("userId") Integer userId);

    // 查询一个用户创建的所有竞赛
    @Select("select competition_id, title from competition where user_id = #{userId}")
    List<AllCompetitionsDTO> getAllCreatedCompetitions(@Param("userId") Integer userId);

    // 查询一个用户参加的所有竞赛
    @Select("select c.competition_id, c.title from competition c " +
            "join competition_member cm on c.competition_id = cm.competition_id " +
            "where cm.user_id = #{userId} " +
            "and cm.admission_status = 1")
    List<AllCompetitionsDTO> getAllAppliedCompetitions(@Param("userId") Integer userId);

    @Delete("delete from competition_member where competition_id = #{competitionId}")
    void deleteMembers(Integer competitionId);

    @Delete("delete from competition where competition_id = #{competitionId}")
    void deleteCompetition(Integer competitionId);

    @Select("select user_id from competition_member where competition_id = #{competitionId}")
    List<Integer> getUserIdsByCompetitionId(Integer competitionId);

    @Select("select title from competition where competition_id = #{competitionId}")
    String getTitleById(Integer competitionId);
}
