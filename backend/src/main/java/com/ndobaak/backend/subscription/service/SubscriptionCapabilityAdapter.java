package com.ndobaak.backend.subscription.service;

import com.ndobaak.backend.common.enums.SubscriptionStatus;
import com.ndobaak.backend.subscription.entity.Subscription;
import com.ndobaak.backend.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionCapabilityAdapter implements LandlordCapabilityPolicy {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionCapabilityAdapter(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(Long landlordId) {
        return subscriptionRepository.existsByLandlordIdAndStatus(landlordId, SubscriptionStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public int getMaxListings(Long landlordId) {
        return subscriptionRepository.findByLandlordIdAndStatus(landlordId, SubscriptionStatus.ACTIVE)
                .map(Subscription::getMaxListings)
                .orElse(0);
    }
}
