package com.ndobaak.backend.subscription.dto;

import com.ndobaak.backend.subscription.entity.Subscription;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    public SubscriptionResponse toResponse(Subscription subscription) {
        if (subscription == null) {
            return null;
        }
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .maxListings(subscription.getMaxListings())
                .amount(subscription.getAmount())
                .autoRenew(subscription.isAutoRenew())
                .daysRemaining(subscription.getDaysRemaining())
                .paymentReference(subscription.getLastPayment() != null
                        ? subscription.getLastPayment().getTransactionId() : null)
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
