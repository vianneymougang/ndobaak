package com.ndobaak.backend.notification.service;

import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.notification.config.TwilioProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsNotificationServiceTest {

    @Mock
    private TwilioProperties twilioProperties;

    private SmsNotificationService buildService(boolean enabled) {
        when(twilioProperties.isEnabled()).thenReturn(enabled);
        if (enabled) {
            when(twilioProperties.getAccountSid()).thenReturn(null);
        }
        return new SmsNotificationService(twilioProperties);
    }

    @Test
    void sendNotificationSms_doesNotThrowWhenDisabled() {
        SmsNotificationService service = buildService(false);

        service.sendNotificationSms("+237600000001", NotificationType.BOOKING_CONFIRMED,
                Map.of("listingTitle", "Studio A", "startDate", "01/04/2026", "endDate", "30/04/2026"));
    }

    @Test
    void sendNotificationSms_doesNotThrowWhenTypeHasNoSmsBody() {
        SmsNotificationService service = buildService(false);

        service.sendNotificationSms("+237600000001", NotificationType.BOOKING_CREATED, Map.of());
    }

    @Test
    void sendNotificationSms_logsAndSkipsWhenDisabledAndBodyExists() {
        SmsNotificationService service = buildService(false);

        service.sendNotificationSms("+237600000001", NotificationType.USER_VERIFIED, Map.of());
    }

    @Test
    void sendNotificationSms_doesNotThrowWhenSmsBodyIsNull() {
        SmsNotificationService service = buildService(false);

        service.sendNotificationSms("+237600000001", NotificationType.LISTING_APPROVED, Map.of());
    }

    @Test
    void sendNotificationSms_doesNotThrowForSubscriptionExpiringSoon() {
        SmsNotificationService service = buildService(false);

        service.sendNotificationSms("+237600000001", NotificationType.SUBSCRIPTION_EXPIRING_SOON,
                Map.of("daysRemaining", "3"));
    }
}
