package com.yuki.webapp.mapper;

import com.yuki.webapp.pojo.profile.UserProfileRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserProfileMapper {

    @Select("""
            SELECT
              profile_id AS profileId,
              user_id AS userId,
              score_tech_depth AS scoreTechDepth,
              score_competition AS scoreCompetition,
              score_teamwork AS scoreTeamwork,
              score_learning AS scoreLearning,
              score_availability AS scoreAvailability,
              composite_score AS compositeScore,
              ability_summary AS abilitySummary,
              weekly_hours AS weeklyHours,
              available_periods AS availablePeriods,
              busy_level AS busyLevel,
              raw_input_snapshot AS rawInputSnapshot,
              llm_output_snapshot AS llmOutputSnapshot,
              generated_at AS generatedAt
            FROM user_profile
            WHERE user_id = #{userId}
            """)
    UserProfileRecord selectByUserId(@Param("userId") int userId);

    @Insert("""
            INSERT INTO user_profile (
              user_id,
              score_tech_depth,
              score_competition,
              score_teamwork,
              score_learning,
              score_availability,
              ability_summary,
              weekly_hours,
              available_periods,
              busy_level,
              raw_input_snapshot,
              llm_output_snapshot,
              generated_at
            ) VALUES (
              #{userId},
              #{scoreTechDepth},
              #{scoreCompetition},
              #{scoreTeamwork},
              #{scoreLearning},
              #{scoreAvailability},
              #{abilitySummary},
              #{weeklyHours},
              #{availablePeriods},
              #{busyLevel},
              #{rawInputSnapshot},
              #{llmOutputSnapshot},
              #{generatedAt}
            )
            ON DUPLICATE KEY UPDATE
              score_tech_depth = VALUES(score_tech_depth),
              score_competition = VALUES(score_competition),
              score_teamwork = VALUES(score_teamwork),
              score_learning = VALUES(score_learning),
              score_availability = VALUES(score_availability),
              ability_summary = VALUES(ability_summary),
              weekly_hours = VALUES(weekly_hours),
              available_periods = VALUES(available_periods),
              busy_level = VALUES(busy_level),
              raw_input_snapshot = VALUES(raw_input_snapshot),
              llm_output_snapshot = VALUES(llm_output_snapshot),
              generated_at = VALUES(generated_at)
            """)
    void upsertProfile(
            @Param("userId") int userId,
            @Param("scoreTechDepth") int scoreTechDepth,
            @Param("scoreCompetition") int scoreCompetition,
            @Param("scoreTeamwork") int scoreTeamwork,
            @Param("scoreLearning") int scoreLearning,
            @Param("scoreAvailability") int scoreAvailability,
            @Param("abilitySummary") String abilitySummary,
            @Param("weeklyHours") Integer weeklyHours,
            @Param("availablePeriods") String availablePeriods,
            @Param("busyLevel") String busyLevel,
            @Param("rawInputSnapshot") String rawInputSnapshot,
            @Param("llmOutputSnapshot") String llmOutputSnapshot,
            @Param("generatedAt") LocalDateTime generatedAt
    );
}

