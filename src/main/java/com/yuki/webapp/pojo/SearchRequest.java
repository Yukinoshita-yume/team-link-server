package com.yuki.webapp.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 竞赛搜索请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    private String query;           // 自然语言搜索词，如"找需要Java+Vue的计算机设计赛"
    private Integer page = 1;
    private Integer pageSize = 10;
    private String deadline;        // 截止日期过滤，格式 yyyy-MM-dd，可为null
    private Integer difficultyMax;  // 难度上限 1-5，可为null
}
