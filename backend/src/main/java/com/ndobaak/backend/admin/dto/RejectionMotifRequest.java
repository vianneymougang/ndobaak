package com.ndobaak.backend.admin.dto;

import com.ndobaak.backend.common.enums.RejectionMotif;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectionMotifRequest {

    @NotNull
    private RejectionMotif motif;

    private String additionalReason;
}
