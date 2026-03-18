package com.ndobaak.backend.review.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndobaak.backend.common.enums.ReviewStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {

    private Long id;
    private String studentName;
    private String studentAvatar;
    private Integer rating;
    private String comment;
    private List<String> photoUrls;
    private ReviewStatus status;
    private LocalDateTime createdAt;
}
