package com.ndobaak.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndobaak.backend.common.enums.UserRole;
import com.ndobaak.backend.common.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserDetailResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private boolean deactivated;
    private String deactivationReason;
    private LocalDateTime registrationDate;
    private String profilePhotoUrl;
    private Long listingCount;
    private Long activeListingCount;
    private BigDecimal totalRevenue;
    private Long bookingCount;
    private Long completedBookingCount;
    private Long reviewCount;
}
