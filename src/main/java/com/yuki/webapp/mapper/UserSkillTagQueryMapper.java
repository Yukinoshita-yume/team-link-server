package com.yuki.webapp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserSkillTagQueryMapper {

    @Select("""
            SELECT t.tag_name
            FROM user_skill_tag ut
            JOIN skill_tag t ON t.tag_id = ut.tag_id
            WHERE ut.user_id = #{userId}
            ORDER BY ut.confidence DESC, t.sort_order ASC, t.tag_id ASC
            """)
    List<String> selectTagNamesByUserId(@Param("userId") int userId);
}

