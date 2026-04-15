package com.ndobaak.backend.auth.dto;

import com.ndobaak.backend.common.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserProfileResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String profilePhotoUrl;
    private UserRole role;
    private LocalDateTime registrationDate;

    private String matricule;
    private String fieldOfStudy;
    private Integer yearOfStudy;

    private Boolean verified;

    private String idCardNumber;
    private String taxId;
    private String mobileMoneyPhone;
    private String notchPayAccountId;
}
