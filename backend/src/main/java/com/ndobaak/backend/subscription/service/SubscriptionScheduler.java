package com.ndobaak.backend.subscription.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    public SubscriptionScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void deactivateExpiredSubscriptions() {
        subscriptionService.deactivateExpiredSubscriptions();
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendExpiryReminders() {
        subscriptionService.sendExpiryReminders();
    }
}
