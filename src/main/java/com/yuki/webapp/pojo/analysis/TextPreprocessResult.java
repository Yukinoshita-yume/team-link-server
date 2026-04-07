package com.yuki.webapp.pojo.analysis;

import lombok.Data;

import java.util.List;

/** 预处理阶段输出：原文、清洗后全文、按标点粗分的句子列表。 */
@Data
public class TextPreprocessResult {
    private String originalText;
    private String cleanedText;
    private List<String> segments;
}
