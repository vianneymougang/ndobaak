package com.ndobaak.backend.notification.service;

import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.notification.dto.NotificationTypePreferenceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationPreferenceService preferenceService;
    @Mock
    private EmailNotificationService emailService;
    @Mock
    private SmsNotificationService smsService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationDispatcher dispatcher;

    private Student buildUser(Long id, String email, String phone) {
        return Student.builder()
                .firstName("Alice")
                .lastName("Doe")
                .email(email)
                .phone(phone)
                .build();
    }

    private NotificationTypePreferenceResponse buildPref(NotificationType type, boolean email, boolean sms) {
        return NotificationTypePreferenceResponse.builder()
                .notificationType(type)
                .emailEnabled(email)
                .smsEnabled(sms)
                .inAppEnabled(true)
                .critical(false)
                .build();
    }

    @Test
    void dispatchCriticalType_alwaysSendsInAppAndEmail() {
        Student user = buildUser(1L, "alice@test.com", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        dispatcher.dispatch(1L, NotificationType.BOOKING_CONFIRMED,
                "Réservation confirmée", "Votre réservation est confirmée.",
                10L, "BOOKING", Map.of(), Map.of());

        verify(notificationService).createNotification(eq(1L), eq(NotificationType.BOOKING_CONFIRMED),
                any(), any(), eq(10L), eq("BOOKING"));
        verify(emailService).sendNotificationEmail(eq("alice@test.com"), eq(NotificationType.BOOKING_CONFIRMED), any());
    }

    @Test
    void dispatchCriticalType_sendsSmsWhenPhonePresent() {
        Student user = buildUser(1L, "alice@test.com", "+237600000001");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        dispatcher.dispatch(1L, NotificationType.BOOKING_CONFIRMED,
                "Réservation confirmée", "Votre réservation est confirmée.",
                10L, "BOOKING", Map.of(), Map.of("listingTitle", "Chambre A", "startDate", "01/04/2026", "endDate", "30/04/2026"));

        verify(smsService).sendNotificationSms(eq("+237600000001"), eq(NotificationType.BOOKING_CONFIRMED), any());
    }

    @Test
    void dispatchCriticalType_doesNotSendSmsWhenPhoneNull() {
        Student user = buildUser(1L, "alice@test.com", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        dispatcher.dispatch(1L, NotificationType.BOOKING_CONFIRMED,
                "Réservation confirmée", "Votre réservation est confirmée.",
                10L, "BOOKING", Map.of(), Map.of());

        verify(smsService, never()).sendNotificationSms(any(), any(), any());
    }

    @Test
    void dispatchCriticalType_doesNotSendSmsWhenPhoneBlank() {
        Student user = buildUser(1L, "alice@test.com", "   ");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        dispatcher.dispatch(1L, NotificationType.BOOKING_CONFIRMED,
                "Réservation confirmée", "Votre réservation est confirmée.",
                10L, "BOOKING", Map.of(), Map.of());

        verify(smsService, never()).sendNotificationSms(any(), any(), any());
    }

    @Test
    void dispatchCriticalType_doesNotConsultPreferences() {
        Student user = buildUser(1L, "alice@test.com", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        dispatcher.dispatch(1L, NotificationType.USER_VERIFIED,
                "Compte vérifié", "Votre compte est vérifié.",
                null, null, Map.of(), Map.of());

        verify(preferenceService, never()).getPreferences(anyLong());
    }

    @Test
    void dispatchNonCriticalType_sendsEmailWhenPreferenceEnabled() {
        Student user = buildUser(1L, "alice@test.com", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceService.getPreferences(1L)).thenReturn(
                List.of(buildPref(NotificationType.BOOKING_CREATED, true, false)));

        dispatcher.dispatch(1L, NotificationType.BOOKING_CREATED,
                "Nouvelle réservation", "Une réservation a été reçue.",
                5L, "BOOKING", Map.of(), Map.of());

        verify(emailService).sendNotificationEmail(eq("alice@test.com"), eq(NotificationType.BOOKING_CREATED), any());
    }

    @Test
    void dispatchNonCriticalType_doesNotSendEmailWhenPreferenceDisabled() {
        Student user = buildUser(1L, "alice@test.com", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceService.getPreferences(1L)).thenReturn(
                List.of(buildPref(NotificationType.BOOKING_CREATED, false, false)));

        dispatcher.dispatch(1L, NotificationType.BOOKING_CREATED,
                "Nouvelle réservation", "Une réservation a été reçue.",
                5L, "BOOKING", Map.of(), Map.of());

        verify(emailService, never()).sendNotificationEmail(any(), any(), any());
    }

    @Test
    void dispatchNonCriticalType_sendsSmsWhenPreferenceEnabledAndPhonePresent() {
        Student user = buildUser(1L, "alice@test.com", "+237600000002");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceService.getPreferences(1L)).thenReturn(
                List.of(buildPref(NotificationType.PAYMENT_FAILED, true, true)));

        dispatcher.dispatch(1L, NotificationType.PAYMENT_FAILED,
                "Échec du paiement", "Votre paiement a échoué.",
                7L, "PAYMENT", Map.of(), Map.of());

        verify(smsService).sendNotificationSms(eq("+237600000002"), eq(NotificationType.PAYMENT_FAILED), any());
    }

    @Test
    void dispatchNonCriticalType_doesNotSendSmsWhenPreferenceEnabledButPhoneNull() {
        Student user = buildUser(1L, "alice@test.com", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceService.getPreferences(1L)).thenReturn(
                List.of(buildPref(NotificationType.PAYMENT_FAILED, true, true)));

        dispatcher.dispatch(1L, NotificationType.PAYMENT_FAILED,
                "Échec du paiement", "Votre paiement a échoué.",
                7L, "PAYMENT", Map.of(), Map.of());

        verify(smsService, never()).sendNotificationSms(any(), any(), any());
    }

    @Test
    void dispatchNonCriticalType_fallsBackToDefaultEmailBehaviorWhenNoPrefFound() {
        Student user = buildUser(1L, "alice@test.com", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceService.getPreferences(1L)).thenReturn(List.of());

        dispatcher.dispatch(1L, NotificationType.LISTING_APPROVED,
                "Annonce approuvée", "Votre annonce a été approuvée.",
                3L, "LISTING", Map.of(), Map.of());

        verify(emailService).sendNotificationEmail(eq("alice@test.com"), eq(NotificationType.LISTING_APPROVED), any());
    }

    @Test
    void dispatchNonCriticalType_noEmailFallbackForTypeNotInDefaultList() {
        Student user = buildUser(1L, "alice@test.com", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceService.getPreferences(1L)).thenReturn(List.of());

        dispatcher.dispatch(1L, NotificationType.REVIEW_POSTED,
                "Avis publié", "Un avis a été publié.",
                null, null, Map.of(), Map.of());

        verify(emailService, never()).sendNotificationEmail(any(), any(), any());
    }

    @Test
    void dispatchAlwaysCreatesInAppNotification() {
        Student user = buildUser(1L, "alice@test.com", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceService.getPreferences(1L)).thenReturn(List.of());

        dispatcher.dispatch(1L, NotificationType.REVIEW_POSTED,
                "Avis publié", "Un avis a été publié.",
                null, null, Map.of(), Map.of());

        verify(notificationService).createNotification(eq(1L), eq(NotificationType.REVIEW_POSTED),
                eq("Avis publié"), eq("Un avis a été publié."), eq(null), eq(null));
    }

    @Test
    void dispatchThrowsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dispatcher.dispatch(99L, NotificationType.BOOKING_CONFIRMED,
                "Titre", "Message", null, null, Map.of(), Map.of()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
