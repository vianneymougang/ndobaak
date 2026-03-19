package com.ndobaak.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportReviewRequest {

    @NotBlank
    @Size(max = 500)
    private String reason;
}
