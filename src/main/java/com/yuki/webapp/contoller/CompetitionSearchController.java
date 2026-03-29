package com.yuki.webapp.contoller;

import com.yuki.webapp.pojo.CompetitionSearchResult;
import com.yuki.webapp.pojo.Result;
import com.yuki.webapp.service.CompetitionSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/index")
public class CompetitionSearchController {

    @Autowired
    private CompetitionSearchService competitionSearchService;

    /**
     * AI 自然语言搜索竞赛
     * GET /index/aiSearch?q=找需要Java+Vue的计算机设计赛
     */
    @GetMapping("/aiSearch")
    public Result<List<CompetitionSearchResult>> aiSearch(@RequestParam("q") String query) {
        if (query == null || query.isBlank()) {
            return Result.error("搜索词不能为空");
        }
        List<CompetitionSearchResult> results = competitionSearchService.search(query);
        return Result.success(results);
    }
}
