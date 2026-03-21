package com.ndobaak.backend.subscription.controller;

import com.ndobaak.backend.subscription.dto.SubscribeRequest;
import com.ndobaak.backend.subscription.dto.SubscriptionResponse;
import com.ndobaak.backend.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscriptions")
@PreAuthorize("hasRole('LANDLORD')")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> subscribe(@Valid @RequestBody SubscribeRequest request) {
        return ResponseEntity.ok(subscriptionService.subscribe(request));
    }

    @GetMapping("/my")
    public ResponseEntity<SubscriptionResponse> getMySubscription() {
        SubscriptionResponse response = subscriptionService.getMySubscription();
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/renew")
    public ResponseEntity<SubscriptionResponse> renew(@Valid @RequestBody SubscribeRequest request) {
        return ResponseEntity.ok(subscriptionService.renew(request));
    }

    @PutMapping("/cancel-auto-renew")
    public ResponseEntity<SubscriptionResponse> cancelAutoRenew() {
        return ResponseEntity.ok(subscriptionService.cancelAutoRenew());
    }
}
