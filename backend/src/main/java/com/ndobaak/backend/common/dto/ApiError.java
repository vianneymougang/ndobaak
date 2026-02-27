package com.ndobaak.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final int status;
    private final String message;
    private final List<FieldError> errors;
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
    }
}
