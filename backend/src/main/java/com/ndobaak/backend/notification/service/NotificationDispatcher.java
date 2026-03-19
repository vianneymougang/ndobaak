package com.ndobaak.backend.notification.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.notification.dto.NotificationTypePreferenceResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationDispatcher {

    private static final Set<NotificationType> DEFAULT_EMAIL_TYPES = Set.of(
            NotificationType.BOOKING_CREATED,
            NotificationType.BOOKING_CANCELLED,
            NotificationType.PAYMENT_FAILED,
            NotificationType.PAYMENT_REMINDER,
            NotificationType.LISTING_APPROVED,
            NotificationType.LISTING_REJECTED,
            NotificationType.SUBSCRIPTION_ACTIVATED,
            NotificationType.SUBSCRIPTION_EXPIRING_SOON,
            NotificationType.ACCOUNT_DEACTIVATED
    );

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final UserRepository userRepository;

    public NotificationDispatcher(NotificationService notificationService,
                                   NotificationPreferenceService preferenceService,
                                   EmailNotificationService emailService,
                                   SmsNotificationService smsService,
                                   UserRepository userRepository) {
        this.notificationService = notificationService;
        this.preferenceService = preferenceService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.userRepository = userRepository;
    }

    public void dispatch(Long userId, NotificationType type, String title, String message,
                         Long referenceId, String referenceType,
                         Map<String, Object> emailVars, Map<String, String> smsVars) {
        notificationService.createNotification(userId, type, title, message, referenceId, referenceType);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Map<String, Object> enrichedVars = new HashMap<>(emailVars);
        enrichedVars.put("notificationTitle", title);
        enrichedVars.put("notificationMessage", message);
        enrichedVars.put("firstName", user.getFirstName());

        if (NotificationPreferenceService.CRITICAL_TYPES.contains(type)) {
            emailService.sendNotificationEmail(user.getEmail(), type, enrichedVars);
            if (user.getPhone() != null && !user.getPhone().isBlank()) {
                smsService.sendNotificationSms(user.getPhone(), type, smsVars);
            }
            return;
        }

        List<NotificationTypePreferenceResponse> prefs = preferenceService.getPreferences(userId);
        Map<NotificationType, NotificationTypePreferenceResponse> prefsMap = prefs.stream()
                .collect(Collectors.toMap(NotificationTypePreferenceResponse::getNotificationType, p -> p));

        NotificationTypePreferenceResponse pref = prefsMap.get(type);
        boolean sendEmail = pref != null ? pref.isEmailEnabled() : DEFAULT_EMAIL_TYPES.contains(type);
        boolean sendSms = pref != null && pref.isSmsEnabled();

        if (sendEmail) {
            emailService.sendNotificationEmail(user.getEmail(), type, enrichedVars);
        }
        if (sendSms && user.getPhone() != null && !user.getPhone().isBlank()) {
            smsService.sendNotificationSms(user.getPhone(), type, smsVars);
        }
    }
}
