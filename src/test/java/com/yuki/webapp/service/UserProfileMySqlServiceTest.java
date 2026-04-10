package com.yuki.webapp.service;

import com.yuki.webapp.mapper.SkillTagMapper;
import com.yuki.webapp.mapper.UserMapper;
import com.yuki.webapp.mapper.UserProfileMapper;
import com.yuki.webapp.mapper.UserSkillTagMapper;
import com.yuki.webapp.mapper.UserSkillTagQueryMapper;
import com.yuki.webapp.pojo.UserDTO;
import com.yuki.webapp.pojo.analysis.FiveDimensionScore;
import com.yuki.webapp.pojo.analysis.ProfileImage;
import com.yuki.webapp.pojo.analysis.ScoreDimension;
import com.yuki.webapp.pojo.analysis.TextAnalysisResult;
import com.yuki.webapp.pojo.profile.CompetenceCardDTO;
import com.yuki.webapp.pojo.profile.UserProfileRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileMySqlServiceTest {

    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private UserSkillTagMapper userSkillTagMapper;
    @Mock
    private UserSkillTagQueryMapper userSkillTagQueryMapper;
    @Mock
    private SkillTagMapper skillTagMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private TextAnalysisService textAnalysisService;

    private UserProfileMySqlService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileMySqlService();
        ReflectionTestUtils.setField(service, "userProfileMapper", userProfileMapper);
        ReflectionTestUtils.setField(service, "userSkillTagMapper", userSkillTagMapper);
        ReflectionTestUtils.setField(service, "userSkillTagQueryMapper", userSkillTagQueryMapper);
        ReflectionTestUtils.setField(service, "skillTagMapper", skillTagMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "textAnalysisService", textAnalysisService);
    }

    @Test
    void getOrGenerate_whenMissing_generatesFromIntroAndReturnsCard() {
        when(userProfileMapper.selectByUserId(1)).thenReturn(null, new UserProfileRecord());

        UserDTO user = new UserDTO();
        user.setUserInformation("熟悉 Java Spring，参加过竞赛。");
        when(userMapper.getUserInfoById(1)).thenReturn(user);

        TextAnalysisResult analysis = new TextAnalysisResult();
        ProfileImage profile = new ProfileImage();
        profile.setSkillTags(List.of("Java", "Spring Boot"));
        profile.setExperienceSummary("能力摘要");
        analysis.setProfileImage(profile);
        FiveDimensionScore score = new FiveDimensionScore();
        score.setTechnicalDepth(new ScoreDimension(80, ""));
        score.setCompetitionExperience(new ScoreDimension(70, ""));
        score.setTeamwork(new ScoreDimension(60, ""));
        score.setLearningAbility(new ScoreDimension(75, ""));
        score.setTimeCommitment(new ScoreDimension(65, ""));
        score.setTotalScore(72.0);
        analysis.setScore(score);
        when(textAnalysisService.analyze(anyString())).thenReturn(analysis);

        when(skillTagMapper.findTagIdByName(anyString())).thenReturn(100);
        when(userSkillTagQueryMapper.selectTagNamesByUserId(1)).thenReturn(List.of("Java"));

        CompetenceCardDTO dto = service.getOrGenerateCompetenceCard(1);

        assertNotNull(dto);
        assertEquals(1, dto.getUserId());
        verify(userProfileMapper, times(1)).upsertProfile(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                any(), any(), any(), any(), any(), any(), any());
        verify(userSkillTagMapper, times(1)).deleteByUserId(1);
    }
}

