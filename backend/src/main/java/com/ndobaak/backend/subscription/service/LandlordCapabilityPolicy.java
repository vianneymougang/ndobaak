package com.ndobaak.backend.subscription.service;

public interface LandlordCapabilityPolicy {

    boolean hasActiveSubscription(Long landlordId);

    int getMaxListings(Long landlordId);
}
