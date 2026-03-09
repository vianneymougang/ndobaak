package com.ndobaak.backend.booking.dto;

import com.ndobaak.backend.common.enums.BookingStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BookingResponse {

    private Long id;
    private ListingSummary listing;
    private StudentSummary student;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private String cancellationReason;
    private LocalDateTime landlordResponseDate;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @Builder
    public static class ListingSummary {
        private Long id;
        private String title;
        private String address;
        private String thumbnailUrl;
        private BigDecimal monthlyPrice;
    }

    @Getter
    @Setter
    @Builder
    public static class StudentSummary {
        private Long id;
        private String firstName;
        private String lastName;
        private String profilePhotoUrl;
    }
}
