package com.ndobaak.backend.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 2, max = 100)
    private String firstName;

    @Size(min = 2, max = 100)
    private String lastName;

    @Size(min = 9, max = 20)
    private String phone;

    @Size(max = 500)
    private String profilePhotoUrl;
}
