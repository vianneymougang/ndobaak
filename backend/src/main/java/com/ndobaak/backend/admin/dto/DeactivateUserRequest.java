package com.ndobaak.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeactivateUserRequest {

    @NotBlank
    private String reason;
}
