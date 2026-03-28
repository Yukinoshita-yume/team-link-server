package com.yuki.webapp;

import com.yuki.webapp.pojo.Competition;
import com.yuki.webapp.pojo.CompetitionSearchResult;
import com.yuki.webapp.service.CompetitionIndexService;
import com.yuki.webapp.service.CompetitionSearchService;
import com.yuki.webapp.utils.DashScopeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class F2IntegrationTest {

    @Autowired
    private DashScopeUtil dashScopeUtil;

    @Autowired
    private CompetitionIndexService competitionIndexService;

    @Autowired
    private CompetitionSearchService competitionSearchService;

    // ── 测试 2.2：Embedding API 是否正常 ──────────────────

    @Test
    void testEmbeddingApi() {
        List<Float> vector = dashScopeUtil.getEmbedding("全国大学生计算机设计大赛 Java Vue");
        assertNotNull(vector, "向量不应为 null");
        assertEquals(1024, vector.size(), "通义千问 text-embedding-v3 应输出 1536 维");
        System.out.println("✅ Embedding 测试通过，维度: " + vector.size());
    }

    // ── 测试 2.2：Chat API 是否正常 ──────────────────────

    @Test
    void testChatApi() {
        String result = dashScopeUtil.chat(
                "你是一个测试助手，只回复「测试成功」四个字。",
                "测试",
                0.1
        );
        assertNotNull(result, "回复不应为 null");
        assertFalse(result.isBlank(), "回复不应为空");
        System.out.println("✅ Chat API 测试通过，回复: " + result);
    }

    // ── 测试 2.2：写入竞赛索引 ────────────────────────────

    @Test
    void testIndexCompetition() {
        Competition competition = new Competition();
        competition.setCompetitionId(9999);
        competition.setTitle("全国大学生计算机设计大赛");
        competition.setTag1("Java");
        competition.setTag2("Vue");
        competition.setTag3("Spring Boot");
        competition.setTag4("前端开发");
        competition.setTag5("Web应用");
        competition.setCompetitionDetails("面向全国在校大学生，考察软件设计与开发能力");
        competition.setMaxParticipants(5);
        competition.setSchoolRequirements("不限");
        competition.setDeadline(LocalDateTime.of(2026, 6, 1, 0, 0));
        competition.setUserId(1);
        competition.setCompetitionCreatedTime(LocalDateTime.now());
        competition.setCompetitionUpdatedTime(LocalDateTime.now());

        // 不抛异常即为成功
        assertDoesNotThrow(() -> competitionIndexService.indexCompetition(competition),
                "写入索引不应抛出异常");
        System.out.println("✅ 竞赛索引写入测试通过");
    }

    // ── 测试 2.3+2.4：混合检索 + Reranking ───────────────

    @Test
    void testSearch() {
        // 先确保有数据（依赖 testIndexCompetition 先跑）
        List<CompetitionSearchResult> results =
                competitionSearchService.search("找需要Java和Vue的计算机设计比赛");

        assertNotNull(results, "搜索结果不应为 null");
        System.out.println("✅ 搜索测试通过，返回 " + results.size() + " 条结果");

        if (!results.isEmpty()) {
            CompetitionSearchResult first = results.get(0);
            System.out.println("  第1条: " + first.getTitle());
            System.out.println("  匹配分: " + first.getMatchScore());
            System.out.println("  命中标签: " + first.getHitTags());

            // 2.5 推荐理由不为空
            assertNotNull(first.getRecommendation(), "推荐理由不应为 null");
            assertFalse(first.getRecommendation().isBlank(), "推荐理由不应为空");
            System.out.println("  推荐理由: " + first.getRecommendation());
        }
    }

    // ── 测试 2.2：删除索引 ────────────────────────────────

    @Test
    void testDeleteIndex() {
        assertDoesNotThrow(() -> competitionIndexService.deleteIndex(9999),
                "删除索引不应抛出异常");
        System.out.println("✅ 竞赛索引删除测试通过");
    }
}
