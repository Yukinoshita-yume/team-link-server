package com.yuki.webapp.pojo.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreDimension {
    private Integer score;
    private String reason;
}
