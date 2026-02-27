package com.ndobaak.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterLandlordRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 100)
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 9, max = 20)
    private String phone;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @Size(max = 50)
    private String idCardNumber;

    @Size(max = 200)
    private String taxId;
}
