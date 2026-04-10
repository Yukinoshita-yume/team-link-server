package com.yuki.webapp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SkillTagMapper {

    @Select("""
            SELECT t.tag_id
            FROM skill_tag t
            WHERE LOWER(t.tag_name) = LOWER(#{text})
            LIMIT 1
            """)
    Integer findTagIdByName(@Param("text") String text);

    @Select("""
            SELECT s.tag_id
            FROM skill_tag_synonym s
            WHERE LOWER(s.synonym_text) = LOWER(#{text})
            LIMIT 1
            """)
    Integer findTagIdBySynonym(@Param("text") String text);

    /**
     * 在 skill_tag.tag_aliases 中模糊匹配（大小写不敏感）。
     */
    @Select("""
            SELECT t.tag_id
            FROM skill_tag t
            WHERE t.tag_aliases IS NOT NULL
              AND LOWER(t.tag_aliases) LIKE CONCAT('%', LOWER(#{text}), '%')
            ORDER BY t.sort_order ASC, t.tag_level DESC, t.tag_id ASC
            LIMIT 1
            """)
    Integer findTagIdByAliasesLike(@Param("text") String text);

    /**
     * 在 skill_tag.tag_keywords 中模糊匹配（大小写不敏感）。
     */
    @Select("""
            SELECT t.tag_id
            FROM skill_tag t
            WHERE t.tag_keywords IS NOT NULL
              AND LOWER(t.tag_keywords) LIKE CONCAT('%', LOWER(#{text}), '%')
            ORDER BY t.sort_order ASC, t.tag_level DESC, t.tag_id ASC
            LIMIT 1
            """)
    Integer findTagIdByKeywordsLike(@Param("text") String text);
}

