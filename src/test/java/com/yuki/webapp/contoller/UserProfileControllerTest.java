package com.yuki.webapp.contoller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuki.webapp.interceptors.LoginInterceptor;
import com.yuki.webapp.pojo.profile.CompetenceCardDTO;
import com.yuki.webapp.pojo.profile.RadarScoresDTO;
import com.yuki.webapp.service.UserProfileQdrantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserProfileController.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserProfileQdrantService userProfileQdrantService;

    @MockBean
    private LoginInterceptor loginInterceptor;

    @BeforeEach
    void stubInterceptor() throws Exception {
        when(loginInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void getCompetenceCard_ok_returnsEnvelope() throws Exception {
        CompetenceCardDTO dto = new CompetenceCardDTO();
        dto.setUserId(1);
        RadarScoresDTO scores = new RadarScoresDTO();
        scores.setTechnicalDepth(80);
        dto.setRadarScores(scores);
        when(userProfileQdrantService.getCompetenceCard(1)).thenReturn(dto);

        mockMvc.perform(get("/user/profile/competence-card")
                        .header("Authorization", "dummy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    void updateSkillTags_ok_returnsSuccess() throws Exception {
        mockMvc.perform(put("/user/profile/skill-tags")
                        .header("Authorization", "dummy-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("skillTags", List.of("Java", "Vue")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}

