package com.ndobaak.backend.subscription.service;

public interface SubscriptionPaymentEventHandler {

    void onPaymentSucceeded(Long paymentId);

    void onPaymentFailed(Long paymentId);
}
