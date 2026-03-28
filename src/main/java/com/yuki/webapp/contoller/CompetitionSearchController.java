package com.yuki.webapp.contoller;

import com.yuki.webapp.pojo.CompetitionSearchResult;
import com.yuki.webapp.pojo.Result;
import com.yuki.webapp.pojo.SearchRequest;
import com.yuki.webapp.service.CompetitionIndexService;
import com.yuki.webapp.service.CompetitionSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG 搜索接口（新增，独立Controller）
 *
 * 注意：同时需要在 WebConfig.java 的 excludePathPatterns 中
 * 将 /search/** 加入白名单（如果允许未登录搜索的话）
 */
@RestController
@RequestMapping("/search")
public class CompetitionSearchController {

    @Autowired
    private CompetitionSearchService searchService;

    @Autowired
    private CompetitionIndexService indexService;

    /**
     * 自然语言搜索竞赛（2.3 + 2.4 + 2.5）
     *
     * POST /search/competitions
     * {
     *   "query": "找需要Java+Vue的计算机设计赛",
     *   "page": 1,
     *   "pageSize": 10
     * }
     *
     * 返回：匹配分、推荐理由、命中标签高亮
     */
    @PostMapping("/competitions")
    public Result<List<CompetitionSearchResult>> searchCompetitions(
            @RequestBody SearchRequest request) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return Result.error("搜索词不能为空");
        }
        List<CompetitionSearchResult> results = searchService.search(request.getQuery());
        return Result.success(results);
    }

    /**
     * 手动触发重建指定竞赛的索引（运营工具接口）
     * POST /search/reindex/{competitionId}
     *
     * 正常情况下索引在竞赛创建时自动写入，此接口用于数据修复
     */
    @PostMapping("/reindex/{competitionId}")
    public Result reindex(@PathVariable Integer competitionId) {
        // 由CompetitionService提供getById方法，此处示意
        // Competition competition = competitionService.getById(competitionId);
        // indexService.indexCompetition(competition);
        return Result.success("触发重建索引：" + competitionId);
    }
}
