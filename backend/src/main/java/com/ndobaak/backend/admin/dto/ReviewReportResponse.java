package com.ndobaak.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewReportResponse {
    private Long id;
    private Long reviewId;
    private String reporterName;
    private Long reporterId;
    private String reason;
    private boolean processed;
    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
