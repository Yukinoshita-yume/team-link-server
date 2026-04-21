package com.yuki.webapp.pojo.review;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ApplicationAIReviewDTO {
    private Integer competitionId;
    private Integer userId;
    private Integer totalScore;
    private String decision;
    private List<ReviewDimensionDTO> dimensions = new ArrayList<>();
    private List<String> highlights = new ArrayList<>();
    private List<String> risks = new ArrayList<>();
    private List<String> interviewQuestions = new ArrayList<>();
}
