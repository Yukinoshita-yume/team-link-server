package com.yuki.webapp.contoller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuki.webapp.interceptors.LoginInterceptor;
import com.yuki.webapp.pojo.analysis.FiveDimensionScore;
import com.yuki.webapp.pojo.analysis.TextAnalysisResult;
import com.yuki.webapp.service.TextAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link TextAnalysisController} 的 Web 层切片测试：Mock 登录拦截器与业务服务。
 */
@WebMvcTest(controllers = TextAnalysisController.class)
class TextAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TextAnalysisService textAnalysisService;

    @MockBean
    private LoginInterceptor loginInterceptor;

    @BeforeEach
    void stubInterceptor() throws Exception {
        when(loginInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void textProfile_ok_returnsResultEnvelope() throws Exception {
        TextAnalysisResult body = new TextAnalysisResult();
        body.setScore(new FiveDimensionScore());
        body.getScore().setTotalScore(72.5);
        when(textAnalysisService.analyze(anyString())).thenReturn(body);

        mockMvc.perform(post("/analysis/text-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("text", "熟悉 Java Spring，参加过程序设计竞赛。"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.score.totalScore").value(72.5));
    }

    @Test
    void textProfile_blankText_validationError() throws Exception {
        mockMvc.perform(post("/analysis/text-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value(containsString("Validation")));
    }
}
