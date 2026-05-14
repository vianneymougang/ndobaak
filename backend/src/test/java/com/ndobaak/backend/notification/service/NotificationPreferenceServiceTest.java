package com.ndobaak.backend.notification.service;

import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.notification.dto.NotificationTypePreferenceResponse;
import com.ndobaak.backend.notification.dto.UpdatePreferenceRequest;
import com.ndobaak.backend.notification.entity.NotificationTypePreference;
import com.ndobaak.backend.notification.repository.NotificationTypePreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationTypePreferenceRepository preferenceRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationPreferenceService preferenceService;

    private Student buildUser(Long id) {
        return Student.builder()
                .firstName("Bob")
                .lastName("Martin")
                .email("bob@test.com")
                .build();
    }

    private NotificationTypePreference buildPref(Student user, NotificationType type, boolean email, boolean sms) {
        return NotificationTypePreference.builder()
                .user(user)
                .notificationType(type)
                .emailEnabled(email)
                .smsEnabled(sms)
                .inAppEnabled(true)
                .build();
    }

    @Test
    void getPreferences_returnsExistingPreferences() {
        Student user = buildUser(1L);
        NotificationTypePreference pref = buildPref(user, NotificationType.BOOKING_CREATED, true, false);
        when(preferenceRepository.findAllByUserId(1L)).thenReturn(List.of(pref));

        List<NotificationTypePreferenceResponse> result = preferenceService.getPreferences(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNotificationType()).isEqualTo(NotificationType.BOOKING_CREATED);
        assertThat(result.get(0).isEmailEnabled()).isTrue();
        assertThat(result.get(0).isSmsEnabled()).isFalse();
    }

    @Test
    void getPreferences_initializesDefaultsWhenNoneExist() {
        Student user = buildUser(1L);
        List<NotificationTypePreference> allPrefs = Arrays.stream(NotificationType.values())
                .map(t -> buildPref(user, t, true, true))
                .collect(Collectors.toList());
        when(preferenceRepository.findAllByUserId(1L))
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(allPrefs);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        List<NotificationTypePreferenceResponse> result = preferenceService.getPreferences(1L);

        assertThat(result).hasSize(NotificationType.values().length);
        verify(preferenceRepository, times(NotificationType.values().length)).save(any());
    }

    @Test
    void getPreferences_marksCriticalTypesCorrectly() {
        Student user = buildUser(1L);
        List<NotificationTypePreference> prefs = List.of(
                buildPref(user, NotificationType.BOOKING_CONFIRMED, true, true),
                buildPref(user, NotificationType.BOOKING_CREATED, true, false));
        when(preferenceRepository.findAllByUserId(1L)).thenReturn(prefs);

        List<NotificationTypePreferenceResponse> result = preferenceService.getPreferences(1L);

        NotificationTypePreferenceResponse confirmed = result.stream()
                .filter(p -> p.getNotificationType() == NotificationType.BOOKING_CONFIRMED)
                .findFirst().orElseThrow();
        NotificationTypePreferenceResponse created = result.stream()
                .filter(p -> p.getNotificationType() == NotificationType.BOOKING_CREATED)
                .findFirst().orElseThrow();

        assertThat(confirmed.isCritical()).isTrue();
        assertThat(created.isCritical()).isFalse();
    }

    @Test
    void updatePreference_updatesEmailAndSmsFields() {
        Student user = buildUser(1L);
        NotificationTypePreference pref = buildPref(user, NotificationType.BOOKING_CREATED, true, false);
        when(preferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.BOOKING_CREATED))
                .thenReturn(Optional.of(pref));
        when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdatePreferenceRequest request = new UpdatePreferenceRequest();
        request.setEmailEnabled(false);
        request.setSmsEnabled(true);

        NotificationTypePreferenceResponse result = preferenceService.updatePreference(1L, NotificationType.BOOKING_CREATED, request);

        assertThat(result.isEmailEnabled()).isFalse();
        assertThat(result.isSmsEnabled()).isTrue();
        verify(preferenceRepository).save(pref);
    }

    @Test
    void updatePreference_createsNewPrefWhenNotFound() {
        Student user = buildUser(1L);
        when(preferenceRepository.findByUserIdAndNotificationType(1L, NotificationType.BOOKING_CREATED))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdatePreferenceRequest request = new UpdatePreferenceRequest();
        request.setEmailEnabled(false);

        NotificationTypePreferenceResponse result = preferenceService.updatePreference(1L, NotificationType.BOOKING_CREATED, request);

        assertThat(result.isEmailEnabled()).isFalse();
        verify(preferenceRepository).save(any(NotificationTypePreference.class));
    }

    @Test
    void updatePreference_throwsBusinessExceptionForCriticalType() {
        UpdatePreferenceRequest request = new UpdatePreferenceRequest();
        request.setEmailEnabled(false);

        assertThatThrownBy(() -> preferenceService.updatePreference(1L, NotificationType.BOOKING_CONFIRMED, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updatePreference_throwsForAllCriticalTypes() {
        UpdatePreferenceRequest request = new UpdatePreferenceRequest();
        request.setSmsEnabled(false);

        NotificationPreferenceService.CRITICAL_TYPES.forEach(type ->
                assertThatThrownBy(() -> preferenceService.updatePreference(1L, type, request))
                        .isInstanceOf(BusinessException.class));
    }

    @Test
    void onUserRegistered_savesAllNotificationTypes() {
        Student user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceRepository.findAllByUserId(1L)).thenReturn(List.of());

        preferenceService.onUserRegistered(new com.ndobaak.backend.auth.event.UserRegisteredEvent(1L));

        verify(preferenceRepository, times(NotificationType.values().length)).save(any());
    }

    @Test
    void onUserRegistered_skipsAlreadyExistingTypes() {
        Student user = buildUser(1L);
        NotificationTypePreference existing = buildPref(user, NotificationType.BOOKING_CREATED, true, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceRepository.findAllByUserId(1L)).thenReturn(List.of(existing));

        preferenceService.onUserRegistered(new com.ndobaak.backend.auth.event.UserRegisteredEvent(1L));

        int expectedSaves = NotificationType.values().length - 1;
        verify(preferenceRepository, times(expectedSaves)).save(any());
    }
}
