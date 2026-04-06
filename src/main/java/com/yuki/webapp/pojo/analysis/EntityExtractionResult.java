package com.yuki.webapp.pojo.analysis;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EntityExtractionResult {
    private List<String> languages = new ArrayList<>();
    private List<String> frameworks = new ArrayList<>();
    private List<String> awards = new ArrayList<>();
}
