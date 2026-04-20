package com.yuki.webapp.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.SearchRequest;
import com.yuki.webapp.mapper.CompetitionMapper;
import com.yuki.webapp.pojo.Competition;
import com.yuki.webapp.pojo.CompetitionSearchResult;
import com.yuki.webapp.utils.DashScopeUtil;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @Autowired
    private CompetitionMapper competitionMapper;

    @Value("${qdrant.collection-name}")
    private String qdrantCollection;

    @Value("${meilisearch.index-name}")
    private String meiliIndex;

    private static final int TOP_CANDIDATES = 20;
    private static final int FINAL_TOP = 10;
    private static final double VECTOR_WEIGHT = 0.7;
    private static final double BM25_WEIGHT = 0.3;
    private static final double TAG_BONUS = 0.25;

    private static final double MIN_HYBRID_SCORE = 0.25;
    private static final double MIN_RERANK_SCORE = 0.25;

    // ── 时间过滤条件内部类 ──────────────────────────────────────────────
    private static class TimeFilter {
        LocalDateTime from;
        LocalDateTime to;
        String cleanQuery;

        boolean hasFilter() {
            return from != null || to != null;
        }
    }

    // ── 全量同步：数据库 → MeiliSearch + Qdrant ────────────────────────
    public String syncAllToIndex() {
        List<Competition> list = competitionMapper.getAllCompetitions();
        int successMeili = 0;
        int successQdrant = 0;

        for (Competition c : list) {
            // 1. 写入 MeiliSearch
            try {
                JSONObject doc = new JSONObject();
                doc.put("competitionId", c.getCompetitionId());
                doc.put("title", c.getTitle());
                doc.put("tag1", c.getTag1());
                doc.put("tag2", c.getTag2());
                doc.put("tag3", c.getTag3());
                doc.put("tag4", c.getTag4());
                doc.put("tag5", c.getTag5());
                doc.put("competitionDetails", c.getCompetitionDetails());
                doc.put("maxParticipants", c.getMaxParticipants());
                doc.put("schoolRequirements", c.getSchoolRequirements());
                doc.put("deadline", c.getDeadline() != null ? c.getDeadline().toString() : null);

                meilisearchClient.getIndex(meiliIndex)
                        .addDocuments("[" + doc.toJSONString() + "]", "competitionId");
                successMeili++;
            } catch (Exception e) {
                System.out.println("[SYNC] MeiliSearch 写入失败 id=" + c.getCompetitionId() + " " + e.getMessage());
            }

            // 2. 生成向量并写入 Qdrant
            try {
                String text = nullSafe(c.getTitle()) + " " +
                        nullSafe(c.getTag1()) + " " +
                        nullSafe(c.getTag2()) + " " +
                        nullSafe(c.getTag3()) + " " +
                        nullSafe(c.getTag4()) + " " +
                        nullSafe(c.getTag5()) + " " +
                        nullSafe(c.getCompetitionDetails());

                List<Float> vector = dashScopeUtil.getEmbedding(text);

                Map<String, JsonWithInt.Value> payload = new HashMap<>();
                payload.put("competitionId", JsonWithInt.Value.newBuilder().setIntegerValue(c.getCompetitionId()).build());
                payload.put("title", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getTitle())).build());
                payload.put("tag1", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getTag1())).build());
                payload.put("tag2", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getTag2())).build());
                payload.put("tag3", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getTag3())).build());
                payload.put("tag4", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getTag4())).build());
                payload.put("tag5", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getTag5())).build());
                payload.put("competitionDetails", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getCompetitionDetails())).build());
                payload.put("deadline", JsonWithInt.Value.newBuilder().setStringValue(
                        c.getDeadline() != null ? c.getDeadline().toString() : "").build());
                payload.put("schoolRequirements", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getSchoolRequirements())).build());
                if (c.getMaxParticipants() != null) {
                    payload.put("maxParticipants", JsonWithInt.Value.newBuilder().setIntegerValue(c.getMaxParticipants()).build());
                }

                Points.PointStruct point = Points.PointStruct.newBuilder()
                        .setId(Points.PointId.newBuilder().setNum(c.getCompetitionId()).build())
                        .setVectors(Points.Vectors.newBuilder()
                                .setVector(Points.Vector.newBuilder().addAllData(vector).build()))
                        .putAllPayload(payload)
                        .build();

                qdrantClient.upsertAsync(qdrantCollection,
                        Collections.singletonList(point)).get();
                successQdrant++;
            } catch (Exception e) {
                System.out.println("[SYNC] Qdrant 写入失败 id=" + c.getCompetitionId() + " " + e.getMessage());
            }
        }

        return "同步完成：共 " + list.size() + " 条，MeiliSearch 成功 " + successMeili
                + " 条，Qdrant 成功 " + successQdrant + " 条";
    }

    // ── 单条写入索引（新建竞赛时调用）────────────────────────────────────
    public void addToIndex(Competition c) {
        // MeiliSearch
        try {
            JSONObject doc = new JSONObject();
            doc.put("competitionId", c.getCompetitionId());
            doc.put("title", c.getTitle());
            doc.put("tag1", c.getTag1());
            doc.put("tag2", c.getTag2());
            doc.put("tag3", c.getTag3());
            doc.put("tag4", c.getTag4());
            doc.put("tag5", c.getTag5());
            doc.put("competitionDetails", c.getCompetitionDetails());
            doc.put("maxParticipants", c.getMaxParticipants());
            doc.put("schoolRequirements", c.getSchoolRequirements());
            doc.put("deadline", c.getDeadline() != null ? c.getDeadline().toString() : null);

            meilisearchClient.getIndex(meiliIndex)
                    .addDocuments("[" + doc.toJSONString() + "]", "competitionId");
        } catch (Exception e) {
            System.out.println("[INDEX] MeiliSearch 单条写入失败 id=" + c.getCompetitionId() + " " + e.getMessage());
        }

        // Qdrant
        try {
            String text = nullSafe(c.getTitle()) + " " +
                    nullSafe(c.getTag1()) + " " +
                    nullSafe(c.getTag2()) + " " +
                    nullSafe(c.getTag3()) + " " +
                    nullSafe(c.getTag4()) + " " +
                    nullSafe(c.getTag5()) + " " +
                    nullSafe(c.getCompetitionDetails());

            List<Float> vector = dashScopeUtil.getEmbedding(text);

            Map<String, JsonWithInt.Value> payload = new HashMap<>();
            payload.put("competitionId", JsonWithInt.Value.newBuilder().setIntegerValue(c.getCompetitionId()).build());
            payload.put("title", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getTitle())).build());
            payload.put("tag1", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getTag1())).build());
            payload.put("tag2", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getTag2())).build());
            payload.put("tag3", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getTag3())).build());
            payload.put("tag4", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getTag4())).build());
            payload.put("tag5", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getTag5())).build());
            payload.put("competitionDetails", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getCompetitionDetails())).build());
            payload.put("deadline", JsonWithInt.Value.newBuilder().setStringValue(
                    c.getDeadline() != null ? c.getDeadline().toString() : "").build());
            payload.put("schoolRequirements", JsonWithInt.Value.newBuilder().setStringValue(nullSafe(c.getSchoolRequirements())).build());
            if (c.getMaxParticipants() != null) {
                payload.put("maxParticipants", JsonWithInt.Value.newBuilder().setIntegerValue(c.getMaxParticipants()).build());
            }

            Points.PointStruct point = Points.PointStruct.newBuilder()
                    .setId(Points.PointId.newBuilder().setNum(c.getCompetitionId()).build())
                    .setVectors(Points.Vectors.newBuilder()
                            .setVector(Points.Vector.newBuilder().addAllData(vector).build()))
                    .putAllPayload(payload)
                    .build();

            qdrantClient.upsertAsync(qdrantCollection, Collections.singletonList(point)).get();
        } catch (Exception e) {
            System.out.println("[INDEX] Qdrant 单条写入失败 id=" + c.getCompetitionId() + " " + e.getMessage());
        }
    }

    // ── 搜索主流程 ────────────────────────────────────────────────────
    public List<CompetitionSearchResult> search(String originalQuery) {
        TimeFilter timeFilter = extractTimeFilter(originalQuery);
        String expandedQuery = rewriteQuery(timeFilter.cleanQuery);

        List<ScoredResult> vectorResults = vectorSearch(expandedQuery, TOP_CANDIDATES);
        List<ScoredResult> bm25Results = bm25Search(expandedQuery, TOP_CANDIDATES);
        List<ScoredResult> merged = hybridMerge(vectorResults, bm25Results);

        boostByTagMatch(merged, timeFilter.cleanQuery);

        List<ScoredResult> reranked = rerank(timeFilter.cleanQuery, merged);

        return reranked.stream()
                .filter(r -> !isExpired(r.deadline))
                .filter(r -> matchesTimeFilter(r.deadline, timeFilter))
                .filter(r -> isRelevant(r))
                .limit(FINAL_TOP)
                .map(r -> buildResult(r, originalQuery))
                .collect(Collectors.toList());
    }

    private boolean isRelevant(ScoredResult r) {
        if (r.rerankScore > 0) return true;
        return r.hybridScore >= MIN_HYBRID_SCORE;
    }

    // ── 时间条件提取 ──────────────────────────────────────────────────
    private TimeFilter extractTimeFilter(String query) {
        TimeFilter filter = new TimeFilter();
        filter.cleanQuery = query;

        String systemPrompt = """
                你是时间条件解析助手。从用户的搜索词中提取截止时间范围，输出严格的JSON，不要其他内容。
                格式：{"from":"YYYY-MM-DDTHH:mm:ss","to":"YYYY-MM-DDTHH:mm:ss","cleanQuery":"去掉时间描述后的查询"}
                规则：
                - 如果有年份限制（如"2026年"），from=该年1月1日，to=该年12月31日23:59:59
                - 如果有"X月前截止"，to=该月最后一天23:59:59，from=null
                - 如果有"X月后截止"，from=该月1日，to=null
                - 如果没有任何时间限制，from=null，to=null，cleanQuery与原文相同
                - null 字段输出 null（不要字符串"null"）
                """;

        try {
            String response = dashScopeUtil.chat(systemPrompt, "搜索词：" + query, 0.0);
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');
            if (start == -1 || end == -1) return filter;

            JSONObject json = JSON.parseObject(response.substring(start, end + 1));
            String fromStr = json.getString("from");
            String toStr = json.getString("to");
            String clean = json.getString("cleanQuery");

            if (fromStr != null && !fromStr.equals("null")) filter.from = LocalDateTime.parse(fromStr);
            if (toStr != null && !toStr.equals("null")) filter.to = LocalDateTime.parse(toStr);
            if (clean != null && !clean.isBlank()) filter.cleanQuery = clean;
        } catch (Exception e) {
            // 解析失败不做时间过滤，降级可用
        }
        return filter;
    }

    private boolean matchesTimeFilter(String deadlineStr, TimeFilter filter) {
        if (!filter.hasFilter()) return true;
        if (deadlineStr == null || deadlineStr.isBlank()) return true;
        try {
            LocalDateTime deadline = LocalDateTime.parse(deadlineStr);
            if (filter.from != null && deadline.isBefore(filter.from)) return false;
            if (filter.to != null && deadline.isAfter(filter.to)) return false;
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    // ── 查询改写 ──────────────────────────────────────────────────────
    private String rewriteQuery(String query) {
        String systemPrompt = """
                你是一个竞赛搜索助手。请将用户的搜索词扩展为更完整的搜索短语，
                加入相关技术栈、竞赛类型的同义词和相关词汇，帮助找到更全面的结果。
                只返回扩展后的搜索短语，不要解释，不要标点符号，词语之间用空格分隔，长度控制在50字以内。
                """;
        try {
            return dashScopeUtil.chat(systemPrompt, query, 0.0);
        } catch (Exception e) {
            return query;
        }
    }

    // ── 向量检索 ──────────────────────────────────────────────────────
    private List<ScoredResult> vectorSearch(String query, int limit) {
        try {
            List<Float> queryVector = dashScopeUtil.getEmbedding(query);
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

    // ── BM25 关键词检索 ───────────────────────────────────────────────
    private List<ScoredResult> bm25Search(String query, int limit) {
        try {
            com.meilisearch.sdk.model.Searchable searchable =
                    meilisearchClient.getIndex(meiliIndex)
                            .search(new SearchRequest(query).setLimit(limit));

            List<ScoredResult> list = new ArrayList<>();
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

    // ── 混合融合 ──────────────────────────────────────────────────────
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

    // ── 标签命中加分 ──────────────────────────────────────────────────
    private void boostByTagMatch(List<ScoredResult> results, String query) {
        if (query == null || query.isBlank()) return;
        String lowerQuery = query.toLowerCase();

        for (ScoredResult r : results) {
            long hitCount = Arrays.asList(r.tag1, r.tag2, r.tag3, r.tag4, r.tag5)
                    .stream()
                    .filter(t -> t != null && !t.isBlank())
                    .filter(t -> lowerQuery.contains(t.toLowerCase()))
                    .count();

            if (hitCount > 0) {
                double bonus = Math.min(hitCount * TAG_BONUS, TAG_BONUS * 4);
                r.hybridScore = Math.min(r.hybridScore + bonus, 1.0);
                r.tagHitCount = (int) hitCount;
            }
        }
        results.sort(Comparator.comparingDouble(r -> -r.hybridScore));
    }

    // ── Reranking ─────────────────────────────────────────────────────
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
                重要：如果某个竞赛与用户需求完全不相关，请直接将其从结果中去除，不要排在末尾。
                只返回相关竞赛的 competitionId 的 JSON 数组，格式如：[3,1,5]，不要其他内容。
                """;
        String userMsg = "用户搜索：" + originalQuery + "\n\n候选竞赛：\n" + candidatesDesc;

        try {
            String response = dashScopeUtil.chat(systemPrompt, userMsg, 0.1);
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
            return reranked;
        } catch (Exception e) {
            return candidates;
        }
    }

    // ── 构建返回结果 ──────────────────────────────────────────────────
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

        double baseScore = r.rerankScore > 0 ? r.rerankScore : r.hybridScore;
        double tagBonus = Math.min(r.tagHitCount * 8.0, 24.0);
        double matchScore = Math.min((baseScore * 76) + tagBonus, 99.0);
        result.setMatchScore(matchScore);

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
            return dashScopeUtil.chat(systemPrompt, userMsg, 0.0);
        } catch (Exception e) {
            return "该竞赛与您的搜索需求高度匹配。";
        }
    }

    // ── 工具方法 ──────────────────────────────────────────────────────
    private boolean isExpired(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.isBlank()) return false;
        try {
            return LocalDateTime.parse(deadlineStr).isBefore(LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

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

    private String nullSafe(String s) {
        return s == null ? "" : s;
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

    private static class ScoredResult {
        Integer competitionId;
        String title, tag1, tag2, tag3, tag4, tag5;
        String competitionDetails, deadline, schoolRequirements;
        Integer maxParticipants;
        double vectorScore = 0;
        double bm25Score = 0;
        double hybridScore = 0;
        double rerankScore = 0;
        int tagHitCount = 0;
    }
}