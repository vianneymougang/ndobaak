package com.ndobaak.backend.subscription.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndobaak.backend.common.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionResponse {

    private Long id;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private int maxListings;
    private BigDecimal amount;
    private boolean autoRenew;
    private long daysRemaining;
    private String paymentReference;
    private LocalDateTime createdAt;
}
