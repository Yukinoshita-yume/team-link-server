package com.yuki.webapp.pojo.analysis;

import lombok.Data;

import java.util.List;

@Data
public class TextPreprocessResult {
    private String originalText;
    private String cleanedText;
    private List<String> segments;
}
