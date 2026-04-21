package com.yuki.webapp.service;

import com.yuki.webapp.pojo.review.ApplicationAIReviewDTO;

public interface ApplicationReviewService {
    ApplicationAIReviewDTO review(Integer competitionId, Integer applicantUserId);
}
