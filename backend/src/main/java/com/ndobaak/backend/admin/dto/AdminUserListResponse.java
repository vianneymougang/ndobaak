package com.ndobaak.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndobaak.backend.common.enums.UserRole;
import com.ndobaak.backend.common.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserListResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private boolean deactivated;
    private LocalDateTime registrationDate;
    private Long listingCount;
    private Long bookingCount;
}
