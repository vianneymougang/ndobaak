package com.ndobaak.backend.notification.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.notification.dto.NotificationTypePreferenceResponse;
import com.ndobaak.backend.notification.dto.UpdatePreferenceRequest;
import com.ndobaak.backend.notification.entity.NotificationTypePreference;
import com.ndobaak.backend.notification.repository.NotificationTypePreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationPreferenceService {

    public static final Set<NotificationType> CRITICAL_TYPES = Set.of(
            NotificationType.BOOKING_CONFIRMED,
            NotificationType.PAYMENT_RECEIVED,
            NotificationType.USER_VERIFIED,
            NotificationType.USER_REJECTED,
            NotificationType.SUBSCRIPTION_EXPIRED
    );

    private final NotificationTypePreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public NotificationPreferenceService(NotificationTypePreferenceRepository preferenceRepository,
                                          UserRepository userRepository) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<NotificationTypePreferenceResponse> getPreferences(Long userId) {
        List<NotificationTypePreference> prefs = preferenceRepository.findAllByUserId(userId);
        if (prefs.isEmpty()) {
            initDefaultPreferences(userId);
            prefs = preferenceRepository.findAllByUserId(userId);
        }
        return prefs.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public NotificationTypePreferenceResponse updatePreference(Long userId, NotificationType type,
                                                                UpdatePreferenceRequest request) {
        if (CRITICAL_TYPES.contains(type)) {
            throw new BusinessException("Les notifications critiques ne peuvent pas \u00eatre modifi\u00e9es.");
        }

        NotificationTypePreference pref = preferenceRepository
                .findByUserIdAndNotificationType(userId, type)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    return NotificationTypePreference.builder()
                            .user(user)
                            .notificationType(type)
                            .build();
                });

        if (request.getEmailEnabled() != null) {
            pref.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getSmsEnabled() != null) {
            pref.setSmsEnabled(request.getSmsEnabled());
        }

        preferenceRepository.save(pref);
        return toResponse(pref);
    }

    @Transactional
    public void createDefaultPreferences(Long userId) {
        initDefaultPreferences(userId);
    }

    private void initDefaultPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Set<NotificationType> existing = preferenceRepository.findAllByUserId(userId).stream()
                .map(NotificationTypePreference::getNotificationType)
                .collect(Collectors.toSet());

        Arrays.stream(NotificationType.values())
                .filter(type -> !existing.contains(type))
                .forEach(type -> preferenceRepository.save(
                        NotificationTypePreference.builder()
                                .user(user)
                                .notificationType(type)
                                .build()
                ));
    }

    private NotificationTypePreferenceResponse toResponse(NotificationTypePreference pref) {
        return NotificationTypePreferenceResponse.builder()
                .notificationType(pref.getNotificationType())
                .emailEnabled(pref.isEmailEnabled())
                .smsEnabled(pref.isSmsEnabled())
                .inAppEnabled(pref.isInAppEnabled())
                .critical(CRITICAL_TYPES.contains(pref.getNotificationType()))
                .build();
    }
}
