package com.yuki.webapp.pojo.profile;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 手动修正技能标签的请求体（支持清空列表）。
 */
@Data
public class SkillTagsUpdateRequest {
    private List<String> skillTags = new ArrayList<>();
}
