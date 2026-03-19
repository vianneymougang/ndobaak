package com.ndobaak.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndobaak.backend.common.enums.ReviewStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminReviewResponse {
    private Long id;
    private Integer rating;
    private String comment;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private String studentName;
    private Long studentId;
    private String listingTitle;
    private Long listingId;
    private Long reportCount;
    private List<String> photoUrls;
}
