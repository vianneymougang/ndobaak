package com.ndobaak.backend.notification.service;

import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.notification.config.TwilioProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final TwilioProperties twilioProperties;

    public SmsNotificationService(TwilioProperties twilioProperties) {
        this.twilioProperties = twilioProperties;
        if (twilioProperties.isEnabled()
                && twilioProperties.getAccountSid() != null
                && !twilioProperties.getAccountSid().isBlank()) {
            Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
        }
    }

    @Async
    public void sendNotificationSms(String phoneNumber, NotificationType type, Map<String, String> vars) {
        String body = buildSmsBody(type, vars);
        if (body != null) {
            sendSms(phoneNumber, body);
        }
    }

    private void sendSms(String phoneNumber, String body) {
        if (!twilioProperties.isEnabled()) {
            log.info("SMS disabled. Would send to {}: {}", phoneNumber, body);
            return;
        }
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Message.creator(
                        new PhoneNumber(phoneNumber),
                        new PhoneNumber(twilioProperties.getFromNumber()),
                        body
                ).create();
                return;
            } catch (Exception e) {
                log.error("SMS send attempt {}/3 failed for {}: {}", attempt, phoneNumber, e.getMessage());
            }
        }
    }

    private String buildSmsBody(NotificationType type, Map<String, String> vars) {
        return switch (type) {
            case BOOKING_CONFIRMED -> String.format(
                    "Ndobaak: R\u00e9servation confirm\u00e9e pour %s du %s au %s.",
                    vars.getOrDefault("listingTitle", ""),
                    vars.getOrDefault("startDate", ""),
                    vars.getOrDefault("endDate", ""));
            case PAYMENT_RECEIVED -> String.format(
                    "Ndobaak: Paiement de %s FCFA re\u00e7u. R\u00e9f: %s",
                    vars.getOrDefault("amount", ""),
                    vars.getOrDefault("reference", ""));
            case USER_VERIFIED -> "Ndobaak: Compte v\u00e9rifi\u00e9 ! Connectez-vous sur ndobaak.com";
            case SUBSCRIPTION_EXPIRING_SOON -> String.format(
                    "Ndobaak: Abonnement expire dans %s jours. Renouvelez sur ndobaak.com",
                    vars.getOrDefault("daysRemaining", ""));
            case SUBSCRIPTION_EXPIRED -> "Ndobaak: Abonnement expir\u00e9. Renouvelez sur ndobaak.com";
            default -> null;
        };
    }
}
