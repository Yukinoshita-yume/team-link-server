package com.yuki.webapp.service;

import com.alibaba.fastjson.JSON;
import com.yuki.webapp.pojo.profile.CompetenceCardDTO;
import com.yuki.webapp.pojo.profile.RadarScoresDTO;
import com.yuki.webapp.utils.DashScopeUtil;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将用户能力卡片数据写入 Qdrant：pointId 为 {@code userId}，payload 存完整 JSON，向量由文本摘要生成。
 */
@Service
public class UserProfileQdrantService {

    @Autowired
    private QdrantClient qdrantClient;

    @Autowired
    private DashScopeUtil dashScopeUtil;

    @Value("${qdrant.profile-collection-name:user_profiles}")
    private String profileCollection;

    /**
     * 读取用户能力卡片；若不存在则返回空结构的默认对象（仅含 userId）。
     */
    public CompetenceCardDTO getCompetenceCard(int userId) {
        try {
            List<Points.RetrievedPoint> points = qdrantClient.retrieveAsync(
                    profileCollection,
                    List.of(Points.PointId.newBuilder().setNum(userId).build()),
                    true,
                    false,
                    null
            ).get();
            if (points == null || points.isEmpty()) {
                return emptyCard(userId);
            }
            Map<String, JsonWithInt.Value> payload = points.get(0).getPayloadMap();
            if (payload == null || !payload.containsKey("cardPayload")) {
                return emptyCard(userId);
            }
            String json = payload.get("cardPayload").getStringValue();
            if (json == null || json.isBlank()) {
                return emptyCard(userId);
            }
            CompetenceCardDTO dto = JSON.parseObject(json, CompetenceCardDTO.class);
            if (dto.getUserId() == null) {
                dto.setUserId(userId);
            }
            if (dto.getRadarScores() == null) {
                dto.setRadarScores(new RadarScoresDTO());
            }
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("从 Qdrant 读取用户画像失败: " + e.getMessage(), e);
        }
    }

    /**
     * 全量保存能力卡片（覆盖写入），并刷新向量。
     */
    public void upsertCompetenceCard(CompetenceCardDTO card) {
        if (card.getUserId() == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        int userId = card.getUserId();
        card.setUserId(userId);
        if (card.getRadarScores() == null) {
            card.setRadarScores(new RadarScoresDTO());
        }
        String embeddingText = buildEmbeddingText(card);
        String cardJson = JSON.toJSONString(card);
        try {
            List<Float> vector = dashScopeUtil.getEmbedding(embeddingText);

            Map<String, Object> raw = new HashMap<>();
            raw.put("userId", userId);
            raw.put("cardPayload", cardJson);

            Points.PointStruct point = Points.PointStruct.newBuilder()
                    .setId(Points.PointId.newBuilder().setNum(userId).build())
                    .setVectors(Points.Vectors.newBuilder()
                            .setVector(Points.Vector.newBuilder().addAllData(vector).build())
                            .build())
                    .putAllPayload(toQdrantPayload(raw))
                    .build();

            qdrantClient.upsertAsync(profileCollection, List.of(point)).get();
        } catch (Exception e) {
            throw new RuntimeException("写入 Qdrant 用户画像失败: " + e.getMessage(), e);
        }
    }

    /**
     * 仅更新技能标签，其余字段从现有点合并后回写。
     */
    public void updateSkillTags(int userId, List<String> skillTags) {
        CompetenceCardDTO existing = getCompetenceCard(userId);
        existing.setUserId(userId);
        if (skillTags != null) {
            existing.setSkillTags(skillTags);
        } else {
            existing.setSkillTags(List.of());
        }
        upsertCompetenceCard(existing);
    }

    private static CompetenceCardDTO emptyCard(int userId) {
        CompetenceCardDTO dto = new CompetenceCardDTO();
        dto.setUserId(userId);
        dto.setRadarScores(new RadarScoresDTO());
        return dto;
    }

    private static String buildEmbeddingText(CompetenceCardDTO card) {
        StringBuilder sb = new StringBuilder();
        if (card.getSkillTags() != null) {
            sb.append(String.join(" ", card.getSkillTags())).append(' ');
        }
        if (card.getExpertiseAreas() != null) {
            sb.append(String.join(" ", card.getExpertiseAreas())).append(' ');
        }
        if (card.getLlmSnapshot() != null && !card.getLlmSnapshot().isBlank()) {
            sb.append(card.getLlmSnapshot());
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? "user_profile" : s;
    }

    private static Map<String, JsonWithInt.Value> toQdrantPayload(Map<String, Object> map) {
        Map<String, JsonWithInt.Value> result = new HashMap<>();
        map.forEach((k, v) -> {
            if (v == null) {
                result.put(k, JsonWithInt.Value.newBuilder().setNullValueValue(0).build());
            } else if (v instanceof String s) {
                result.put(k, JsonWithInt.Value.newBuilder().setStringValue(s).build());
            } else if (v instanceof Integer i) {
                result.put(k, JsonWithInt.Value.newBuilder().setIntegerValue(i).build());
            } else {
                result.put(k, JsonWithInt.Value.newBuilder().setStringValue(v.toString()).build());
            }
        });
        return result;
    }
}
