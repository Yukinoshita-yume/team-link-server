package com.yuki.webapp.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserSkillTagMapper {

    @Delete("DELETE FROM user_skill_tag WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") int userId);

    @Insert("""
            INSERT INTO user_skill_tag (user_id, tag_id, confidence, evidence, source, is_confirmed)
            VALUES (#{userId}, #{tagId}, #{confidence}, #{evidence}, 'LLM_EXTRACT', FALSE)
            ON DUPLICATE KEY UPDATE
              confidence = VALUES(confidence),
              evidence = VALUES(evidence),
              source = 'LLM_EXTRACT',
              is_confirmed = FALSE
            """)
    void upsertOne(
            @Param("userId") int userId,
            @Param("tagId") int tagId,
            @Param("confidence") double confidence,
            @Param("evidence") String evidence
    );
}

