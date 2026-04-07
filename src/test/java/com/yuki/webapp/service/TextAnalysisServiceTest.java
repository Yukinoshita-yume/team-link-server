package com.yuki.webapp.service;

import com.yuki.webapp.config.TextAnalysisProperties;
import com.yuki.webapp.pojo.analysis.TextAnalysisResult;
import com.yuki.webapp.utils.DashScopeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TextAnalysisService} 单元测试：Mock LLM，覆盖 LLM 成功路径、异常兜底与评分解释模板。
 */
@ExtendWith(MockitoExtension.class)
class TextAnalysisServiceTest {

    @Mock
    private DashScopeUtil dashScopeUtil;

    private final TextAnalysisProperties textAnalysisProperties = new TextAnalysisProperties();

    private TextAnalysisService textAnalysisService;

    @BeforeEach
    void setUp() {
        textAnalysisService = new TextAnalysisService();
        ReflectionTestUtils.setField(textAnalysisService, "dashScopeUtil", dashScopeUtil);
        ReflectionTestUtils.setField(textAnalysisService, "textAnalysisProperties", textAnalysisProperties);
    }

    @Test
    void analyze_llmReturnsEntityAndProfile_setsLlmSourcesAndScoreExplanation() {
        when(dashScopeUtil.chat(anyString(), anyString(), eq(0.0)))
                .thenReturn("{\"languages\":[\"Java\"],\"frameworks\":[\"Spring\"],\"awards\":[\"省一\"]}");
        when(dashScopeUtil.chat(anyString(), anyString(), eq(0.1)))
                .thenReturn("{\"skillTags\":[\"Java\",\"Spring\"],\"experienceSummary\":\"项目经验丰富\","
                        + "\"technicalDepthEvidence\":\"掌握主流栈\",\"competitionExperienceEvidence\":\"获省一\","
                        + "\"teamworkEvidence\":\"分工明确\",\"learningEvidence\":\"持续学习\",\"timeCommitmentEvidence\":\"每周投入\"}");

        TextAnalysisResult result = textAnalysisService.analyze("我使用 Java 和 Spring 参加竞赛获省一");

        assertEquals("LLM", result.getQuality().getEntitySource());
        assertEquals("LLM", result.getQuality().getProfileSource());
        assertFalse(result.getQuality().isLowConfidence());
        assertTrue(result.getScoreExplanation().contains("综合得分"));
        assertFalse(result.getScoreExplanation().contains("{total}"));
        assertTrue(result.getEntities().getLanguages().stream().anyMatch(s -> s.equalsIgnoreCase("java")));
        verify(dashScopeUtil, times(1)).chat(anyString(), anyString(), eq(0.0));
        verify(dashScopeUtil, times(1)).chat(anyString(), anyString(), eq(0.1));
    }

    @Test
    void analyze_llmThrows_usesRuleFallbackAndMarksLowConfidence() {
        when(dashScopeUtil.chat(anyString(), anyString(), anyDouble())).thenThrow(new RuntimeException("upstream error"));

        TextAnalysisResult result = textAnalysisService.analyze("项目使用 java 和 vue 框架，团队协作良好");

        assertEquals("RULE", result.getQuality().getEntitySource());
        assertEquals("RULE", result.getQuality().getProfileSource());
        assertTrue(result.getQuality().isLowConfidence());
        assertFalse(result.getQuality().getHints().isEmpty());
        assertTrue(result.getEntities().getLanguages().contains("java"));
        assertTrue(result.getEntities().getFrameworks().contains("vue"));
    }

    @Test
    void analyze_customScoreTemplate_replacesPlaceholders() {
        when(dashScopeUtil.chat(anyString(), anyString(), anyDouble())).thenThrow(new RuntimeException("skip llm"));
        textAnalysisProperties.getTemplates().setScoreExplanation("total={total},td={td}");

        TextAnalysisResult result = textAnalysisService.analyze("python django");

        assertTrue(result.getScoreExplanation().startsWith("total="));
        assertFalse(result.getScoreExplanation().contains("{td}"));
    }
}
