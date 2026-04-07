package com.yuki.webapp.pojo.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 单个维度的分数与可读说明。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreDimension {
    private Integer score;
    private String reason;
}
