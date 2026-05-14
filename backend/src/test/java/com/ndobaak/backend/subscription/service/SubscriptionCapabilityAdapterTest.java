package com.ndobaak.backend.subscription.service;

import com.ndobaak.backend.common.enums.SubscriptionStatus;
import com.ndobaak.backend.subscription.entity.Subscription;
import com.ndobaak.backend.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionCapabilityAdapterTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionCapabilityAdapter adapter;

    @Test
    void hasActiveSubscriptionShouldReturnTrueWhenActive() {
        when(subscriptionRepository.existsByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE)).thenReturn(true);

        assertThat(adapter.hasActiveSubscription(10L)).isTrue();
    }

    @Test
    void hasActiveSubscriptionShouldReturnFalseWhenNotActive() {
        when(subscriptionRepository.existsByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE)).thenReturn(false);

        assertThat(adapter.hasActiveSubscription(10L)).isFalse();
    }

    @Test
    void getMaxListingsShouldReturnLimitFromActiveSubscription() {
        Subscription subscription = Subscription.builder()
                .amount(BigDecimal.valueOf(100_000))
                .maxListings(20)
                .build();
        when(subscriptionRepository.findByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));

        assertThat(adapter.getMaxListings(10L)).isEqualTo(20);
    }

    @Test
    void getMaxListingsShouldReturnZeroWhenNoActiveSubscription() {
        when(subscriptionRepository.findByLandlordIdAndStatus(10L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThat(adapter.getMaxListings(10L)).isZero();
    }
}
