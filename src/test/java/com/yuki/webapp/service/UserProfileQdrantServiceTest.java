//package com.yuki.webapp.service;
//
//import com.google.common.util.concurrent.Futures;
//import com.yuki.webapp.pojo.profile.CompetenceCardDTO;
//import com.yuki.webapp.pojo.profile.RadarScoresDTO;
//import com.yuki.webapp.utils.DashScopeUtil;
//import io.qdrant.client.QdrantClient;
//import io.qdrant.client.grpc.JsonWithInt;
//import io.qdrant.client.grpc.Points;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class UserProfileQdrantServiceTest {
//
//    @Mock
//    private QdrantClient qdrantClient;
//
//    @Mock
//    private DashScopeUtil dashScopeUtil;
//
//    private UserProfileQdrantService service;
//
//    @BeforeEach
//    void setUp() {
//        service = new UserProfileQdrantService();
//        ReflectionTestUtils.setField(service, "qdrantClient", qdrantClient);
//        ReflectionTestUtils.setField(service, "dashScopeUtil", dashScopeUtil);
//        ReflectionTestUtils.setField(service, "profileCollection", "user_profiles");
//    }
//
//    @Test
//    void getCompetenceCard_whenNoPoint_returnsEmptyCard() throws Exception {
//        when(qdrantClient.retrieveAsync(anyString(), any(), anyBoolean(), anyBoolean(), isNull()))
//                .thenReturn(Futures.immediateFuture(List.of()));
//
//        CompetenceCardDTO dto = service.getCompetenceCard(123);
//
//        assertEquals(123, dto.getUserId());
//        assertNotNull(dto.getRadarScores());
//        assertTrue(dto.getSkillTags().isEmpty());
//    }
//
//    @Test
//    void getCompetenceCard_whenHasPayload_parsesJson() throws Exception {
//        CompetenceCardDTO origin = new CompetenceCardDTO();
//        origin.setUserId(123);
//        origin.getSkillTags().add("Java");
//        RadarScoresDTO scores = new RadarScoresDTO();
//        scores.setTechnicalDepth(80);
//        origin.setRadarScores(scores);
//
//        Map<String, JsonWithInt.Value> payload = new HashMap<>();
//        payload.put("cardPayload", JsonWithInt.Value.newBuilder()
//                .setStringValue(com.alibaba.fastjson.JSON.toJSONString(origin))
//                .build());
//
//        Points.RetrievedPoint point = Points.RetrievedPoint.newBuilder()
//                .putAllPayload(payload)
//                .build();
//
//        when(qdrantClient.retrieveAsync(anyString(), any(), anyBoolean(), anyBoolean(), isNull()))
//                .thenReturn(Futures.immediateFuture(List.of(point)));
//
//        CompetenceCardDTO dto = service.getCompetenceCard(123);
//
//        assertEquals(123, dto.getUserId());
//        assertEquals(List.of("Java"), dto.getSkillTags());
//        assertEquals(80, dto.getRadarScores().getTechnicalDepth());
//    }
//
//    @Test
//    void upsertCompetenceCard_callsEmbeddingAndUpsert() throws Exception {
//        CompetenceCardDTO card = new CompetenceCardDTO();
//        card.setUserId(123);
//        card.setRadarScores(new RadarScoresDTO());
//        card.getSkillTags().add("Java");
//
//        when(dashScopeUtil.getEmbedding(anyString()))
//                .thenReturn(List.of(0.1f, 0.2f, 0.3f));
//        when(qdrantClient.upsertAsync(anyString(), any()))
//                .thenReturn(Futures.immediateFuture(null));
//
//        service.upsertCompetenceCard(card);
//
//        verify(dashScopeUtil, times(1)).getEmbedding(anyString());
//        verify(qdrantClient, times(1)).upsertAsync(eq("user_profiles"), any());
//    }
//
//    @Test
//    void updateSkillTags_overridesTagsOnly() throws Exception {
//        CompetenceCardDTO origin = new CompetenceCardDTO();
//        origin.setUserId(1);
//        origin.getSkillTags().add("Old");
//        origin.setRadarScores(new RadarScoresDTO());
//        origin.getRadarScores().setTechnicalDepth(60);
//
//        Map<String, JsonWithInt.Value> payload = new HashMap<>();
//        payload.put("cardPayload", JsonWithInt.Value.newBuilder()
//                .setStringValue(com.alibaba.fastjson.JSON.toJSONString(origin))
//                .build());
//        Points.RetrievedPoint point = Points.RetrievedPoint.newBuilder()
//                .putAllPayload(payload)
//                .build();
//
//        when(qdrantClient.retrieveAsync(anyString(), any(), anyBoolean(), anyBoolean(), isNull()))
//                .thenReturn(Futures.immediateFuture(List.of(point)));
//        when(dashScopeUtil.getEmbedding(anyString()))
//                .thenReturn(List.of(0.1f, 0.2f, 0.3f));
//        when(qdrantClient.upsertAsync(anyString(), any()))
//                .thenReturn(Futures.immediateFuture(null));
//
//        service.updateSkillTags(1, List.of("Java", "Vue"));
//
//        verify(qdrantClient, times(1)).upsertAsync(eq("user_profiles"), any());
//    }
//}
//
