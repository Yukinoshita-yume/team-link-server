package com.yuki.webapp.pojo.analysis;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** LLM 或规则词典抽取的实体列表。 */
@Data
public class EntityExtractionResult {
    private List<String> languages = new ArrayList<>();
    private List<String> frameworks = new ArrayList<>();
    private List<String> awards = new ArrayList<>();
}
