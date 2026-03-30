package com.yuki.webapp.service;

import com.alibaba.fastjson.JSON;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.yuki.webapp.pojo.Competition;
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

@Service
public class CompetitionIndexService {

    @Autowired
    private QdrantClient qdrantClient;

    @Autowired
    private Client meilisearchClient;

    @Autowired
    private DashScopeUtil dashScopeUtil;

    @Value("${qdrant.collection-name}")
    private String qdrantCollection;

    @Value("${meilisearch.index-name}")
    private String meiliIndex;

    public void indexCompetition(Competition competition) {
        String text = buildIndexText(competition);
        indexToQdrant(competition, text);
        indexToMeilisearch(competition);
    }

    public void deleteIndex(Integer competitionId) {
        // 1.13.0 正确写法：传 List<PointId>
        try {
            qdrantClient.deleteAsync(
                    qdrantCollection,
                    List.of(Points.PointId.newBuilder()
                            .setNum(competitionId)
                            .build())
            ).get();
        } catch (Exception e) {
            throw new RuntimeException("删除Qdrant索引失败", e);
        }

        try {
            meilisearchClient.getIndex(meiliIndex)
                    .deleteDocument(String.valueOf(competitionId));
        } catch (Exception e) {
            throw new RuntimeException("删除Meilisearch索引失败", e);
        }
    }

    private String buildIndexText(Competition competition) {
        StringBuilder sb = new StringBuilder();
        sb.append(competition.getTitle()).append(" ");
        appendIfNotNull(sb, competition.getTag1());
        appendIfNotNull(sb, competition.getTag2());
        appendIfNotNull(sb, competition.getTag3());
        appendIfNotNull(sb, competition.getTag4());
        appendIfNotNull(sb, competition.getTag5());
        if (competition.getCompetitionDetails() != null) {
            String detail = competition.getCompetitionDetails();
            sb.append(detail, 0, Math.min(detail.length(), 500));
        }
        return sb.toString();
    }

    private void appendIfNotNull(StringBuilder sb, String tag) {
        if (tag != null && !tag.isBlank()) {
            sb.append(tag).append(" ");
        }
    }

    private void indexToQdrant(Competition competition, String text) {
        try {
            List<Float> vector = dashScopeUtil.getEmbedding(text);

            Map<String, Object> payload = new HashMap<>();
            payload.put("competitionId", competition.getCompetitionId());
            payload.put("title", competition.getTitle());
            payload.put("tag1", competition.getTag1());
            payload.put("tag2", competition.getTag2());
            payload.put("tag3", competition.getTag3());
            payload.put("tag4", competition.getTag4());
            payload.put("tag5", competition.getTag5());
            payload.put("deadline", competition.getDeadline() != null
                    ? competition.getDeadline().toString() : null);
            payload.put("maxParticipants", competition.getMaxParticipants());
            payload.put("schoolRequirements", competition.getSchoolRequirements());

            Points.PointStruct point = Points.PointStruct.newBuilder()
                    .setId(Points.PointId.newBuilder()
                            .setNum(competition.getCompetitionId())
                            .build())
                    .setVectors(Points.Vectors.newBuilder()
                            .setVector(Points.Vector.newBuilder()
                                    .addAllData(vector)
                                    .build())
                            .build())
                    .putAllPayload(toQdrantPayload(payload))
                    .build();

            // 1.13.0 正确写法：upsertAsync(collectionName, List<PointStruct>)
            qdrantClient.upsertAsync(qdrantCollection, List.of(point)).get();

        } catch (Exception e) {
            throw new RuntimeException("写入Qdrant失败: " + e.getMessage(), e);
        }
    }

    private void indexToMeilisearch(Competition competition) {
        try {
            Index index = meilisearchClient.getIndex(meiliIndex);
            String jsonDoc = JSON.toJSONString(List.of(competition));
            index.addDocuments(jsonDoc, "competitionId");
        } catch (Exception e) {
            throw new RuntimeException("写入Meilisearch失败: " + e.getMessage(), e);
        }
    }

    private Map<String, JsonWithInt.Value> toQdrantPayload(Map<String, Object> map) {
        Map<String, JsonWithInt.Value> result = new HashMap<>();
        map.forEach((k, v) -> {
            if (v == null) {
                result.put(k, JsonWithInt.Value.newBuilder()
                        .setNullValueValue(0).build());
            } else if (v instanceof String s) {
                result.put(k, JsonWithInt.Value.newBuilder()
                        .setStringValue(s).build());
            } else if (v instanceof Integer i) {
                result.put(k, JsonWithInt.Value.newBuilder()
                        .setIntegerValue(i).build());
            } else {
                result.put(k, JsonWithInt.Value.newBuilder()
                        .setStringValue(v.toString()).build());
            }
        });
        return result;
    }
}
