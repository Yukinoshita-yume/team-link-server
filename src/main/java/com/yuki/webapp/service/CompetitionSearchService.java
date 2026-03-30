package com.yuki.webapp.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.SearchRequest;
import com.yuki.webapp.pojo.CompetitionSearchResult;
import com.yuki.webapp.utils.DashScopeUtil;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompetitionSearchService {

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

    private static final int TOP_CANDIDATES = 20;
    private static final int FINAL_TOP = 5;
    private static final double VECTOR_WEIGHT = 0.7;
    private static final double BM25_WEIGHT = 0.3;

    public List<CompetitionSearchResult> search(String originalQuery) {
        String expandedQuery = rewriteQuery(originalQuery);
        List<ScoredResult> vectorResults = vectorSearch(expandedQuery, TOP_CANDIDATES);
        List<ScoredResult> bm25Results = bm25Search(expandedQuery, TOP_CANDIDATES);
        List<ScoredResult> merged = hybridMerge(vectorResults, bm25Results);
        List<ScoredResult> reranked = rerank(originalQuery, merged);
        return reranked.stream()
                .limit(FINAL_TOP)
                .map(r -> buildResult(r, originalQuery))
                .collect(Collectors.toList());
    }

    // ── 步骤1：查询改写 ──────────────────────────────────

    private String rewriteQuery(String query) {
        String systemPrompt = """
                你是一个竞赛搜索助手。请将用户的搜索词扩展为更完整的搜索短语，
                加入相关技术栈、竞赛类型的同义词和相关词汇，帮助找到更全面的结果。
                只返回扩展后的搜索短语，不要解释，不要标点符号，词语之间用空格分隔，长度控制在50字以内。
                """;
        try {
            return dashScopeUtil.chat(systemPrompt, query, 0.3);
        } catch (Exception e) {
            return query;
        }
    }

    // ── 步骤2：向量检索 ──────────────────────────────────

    private List<ScoredResult> vectorSearch(String query, int limit) {
        try {
            List<Float> queryVector = dashScopeUtil.getEmbedding(query);

            // 1.13.0 修复：searchAsync 返回 List<ScoredPoint>，不是 ListenableFuture<List<Searchable>>
            List<Points.ScoredPoint> points = qdrantClient.searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName(qdrantCollection)
                            .addAllVector(queryVector)
                            .setLimit(limit)
                            .setWithPayload(Points.WithPayloadSelector.newBuilder()
                                    .setEnable(true).build())
                            .build()
            ).get();

            return points.stream().map(p -> {
                ScoredResult r = payloadToResult(p.getPayload());
                r.vectorScore = p.getScore();
                return r;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ── 步骤3：BM25 关键词检索 ──────────────────────────

    private List<ScoredResult> bm25Search(String query, int limit) {
        try {
            // 1.13.0 修复：search() 返回 Searchable，强转为 SearchResult
            com.meilisearch.sdk.model.Searchable searchable =
                    meilisearchClient.getIndex(meiliIndex)
                            .search(new SearchRequest(query).setLimit(limit));

            List<ScoredResult> list = new ArrayList<>();
            // getHits() 返回 List<HashMap>，用 fastjson 统一处理
            JSONArray hits = JSON.parseArray(JSON.toJSONString(searchable.getHits()));
            for (int i = 0; i < hits.size(); i++) {
                JSONObject hit = hits.getJSONObject(i);
                ScoredResult r = new ScoredResult();
                r.competitionId = hit.getInteger("competitionId");
                r.title = hit.getString("title");
                r.tag1 = hit.getString("tag1");
                r.tag2 = hit.getString("tag2");
                r.tag3 = hit.getString("tag3");
                r.tag4 = hit.getString("tag4");
                r.tag5 = hit.getString("tag5");
                r.competitionDetails = hit.getString("competitionDetails");
                r.maxParticipants = hit.getInteger("maxParticipants");
                r.schoolRequirements = hit.getString("schoolRequirements");
                r.deadline = hit.getString("deadline");
                r.bm25Score = 1.0 - (double) i / Math.max(hits.size(), 1);
                list.add(r);
            }
            return list;

        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ── 步骤4：混合融合 ──────────────────────────────────

    private List<ScoredResult> hybridMerge(List<ScoredResult> vectorResults,
                                            List<ScoredResult> bm25Results) {
        Map<Integer, ScoredResult> map = new LinkedHashMap<>();

        for (ScoredResult r : vectorResults) {
            r.hybridScore = r.vectorScore * VECTOR_WEIGHT;
            map.put(r.competitionId, r);
        }
        for (ScoredResult r : bm25Results) {
            if (map.containsKey(r.competitionId)) {
                map.get(r.competitionId).hybridScore += r.bm25Score * BM25_WEIGHT;
            } else {
                r.hybridScore = r.bm25Score * BM25_WEIGHT;
                map.put(r.competitionId, r);
            }
        }

        return map.values().stream()
                .sorted(Comparator.comparingDouble(r -> -r.hybridScore))
                .limit(TOP_CANDIDATES)
                .collect(Collectors.toList());
    }

    // ── 步骤5：Reranking ─────────────────────────────────

    private List<ScoredResult> rerank(String originalQuery, List<ScoredResult> candidates) {
        if (candidates.isEmpty()) return candidates;

        StringBuilder candidatesDesc = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            ScoredResult r = candidates.get(i);
            candidatesDesc.append(i).append(". [").append(r.competitionId).append("] ")
                    .append(r.title).append("，标签：")
                    .append(joinTags(r)).append("\n");
        }

        String systemPrompt = """
                你是一个竞赛推荐助手。根据用户的搜索意图，对候选竞赛按相关性从高到低排序。
                只返回竞赛编号（competitionId）的JSON数组，格式如：[3,1,5,2,4]，不要其他内容。
                """;
        String userMsg = "用户搜索：" + originalQuery + "\n\n候选竞赛：\n" + candidatesDesc;

        try {
            String response = dashScopeUtil.chat(systemPrompt, userMsg, 0.1);
            // 提取 JSON 数组（防止 LLM 在前后加了多余文字）
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start == -1 || end == -1) return candidates;
            JSONArray idArray = JSON.parseArray(response.substring(start, end + 1));

            Map<Integer, ScoredResult> idMap = candidates.stream()
                    .collect(Collectors.toMap(r -> r.competitionId, r -> r));

            List<ScoredResult> reranked = new ArrayList<>();
            for (int i = 0; i < idArray.size(); i++) {
                Integer id = idArray.getInteger(i);
                if (idMap.containsKey(id)) {
                    ScoredResult r = idMap.get(id);
                    r.rerankScore = 1.0 - (double) i / idArray.size();
                    reranked.add(r);
                }
            }
            // 补上 reranking 没返回的条目（防止 LLM 漏掉某些 ID）
            Set<Integer> rerankedIds = reranked.stream()
                    .map(r -> r.competitionId).collect(Collectors.toSet());
            candidates.stream()
                    .filter(r -> !rerankedIds.contains(r.competitionId))
                    .forEach(reranked::add);
            return reranked;
        } catch (Exception e) {
            return candidates;
        }
    }

    // ── 步骤6：生成推荐理由（2.5）────────────────────────

    private CompetitionSearchResult buildResult(ScoredResult r, String originalQuery) {
        CompetitionSearchResult result = new CompetitionSearchResult();
        result.setCompetitionId(r.competitionId);
        result.setTitle(r.title);
        result.setTag1(r.tag1);
        result.setTag2(r.tag2);
        result.setTag3(r.tag3);
        result.setTag4(r.tag4);
        result.setTag5(r.tag5);
        result.setCompetitionDetails(r.competitionDetails);
        result.setMaxParticipants(r.maxParticipants);
        result.setSchoolRequirements(r.schoolRequirements);
        result.setDeadline(r.deadline);

        double score = Math.min(r.rerankScore > 0 ? r.rerankScore : r.hybridScore, 1.0) * 100;
        result.setMatchScore(Math.round(score * 10.0) / 10.0);
        result.setHitTags(findHitTags(r, originalQuery));
        result.setRecommendation(generateRecommendation(r, originalQuery));
        return result;
    }

    private String generateRecommendation(ScoredResult r, String query) {
        String systemPrompt = """
                你是竞赛推荐助手。根据用户搜索词和竞赛信息，生成1-2句推荐理由。
                语言简洁自然，突出与用户需求的匹配点。直接输出推荐理由，不要前缀。
                """;
        String userMsg = String.format("用户搜索：%s\n竞赛名称：%s\n竞赛标签：%s\n竞赛简介：%s",
                query, r.title, joinTags(r),
                r.competitionDetails != null
                        ? r.competitionDetails.substring(0, Math.min(r.competitionDetails.length(), 200))
                        : "");
        try {
            return dashScopeUtil.chat(systemPrompt, userMsg, 0.5);
        } catch (Exception e) {
            return "该竞赛与您的搜索需求高度匹配。";
        }
    }

    // ── 工具方法 ──────────────────────────────────────────

    private List<String> findHitTags(ScoredResult r, String query) {
        String lowerQuery = query.toLowerCase();
        return Arrays.asList(r.tag1, r.tag2, r.tag3, r.tag4, r.tag5)
                .stream()
                .filter(t -> t != null && !t.isBlank())
                .filter(t -> lowerQuery.contains(t.toLowerCase()))
                .collect(Collectors.toList());
    }

    private String joinTags(ScoredResult r) {
        return Arrays.asList(r.tag1, r.tag2, r.tag3, r.tag4, r.tag5)
                .stream()
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining(" "));
    }

    private ScoredResult payloadToResult(Map<String, JsonWithInt.Value> payload) {
        ScoredResult r = new ScoredResult();
        r.competitionId = (int) payload.get("competitionId").getIntegerValue();
        r.title = getStringPayload(payload, "title");
        r.tag1 = getStringPayload(payload, "tag1");
        r.tag2 = getStringPayload(payload, "tag2");
        r.tag3 = getStringPayload(payload, "tag3");
        r.tag4 = getStringPayload(payload, "tag4");
        r.tag5 = getStringPayload(payload, "tag5");
        r.competitionDetails = getStringPayload(payload, "competitionDetails");
        r.deadline = getStringPayload(payload, "deadline");
        r.schoolRequirements = getStringPayload(payload, "schoolRequirements");
        if (payload.containsKey("maxParticipants")) {
            r.maxParticipants = (int) payload.get("maxParticipants").getIntegerValue();
        }
        return r;
    }

    private String getStringPayload(Map<String, JsonWithInt.Value> payload, String key) {
        if (!payload.containsKey(key)) return null;
        String val = payload.get(key).getStringValue();
        return val.isBlank() ? null : val;
    }

    // ── 内部数据结构 ──────────────────────────────────────

    private static class ScoredResult {
        Integer competitionId;
        String title, tag1, tag2, tag3, tag4, tag5;
        String competitionDetails, deadline, schoolRequirements;
        Integer maxParticipants;
        double vectorScore = 0;
        double bm25Score = 0;
        double hybridScore = 0;
        double rerankScore = 0;
    }
}