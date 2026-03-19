package com.ndobaak.backend.notification.service;

import com.ndobaak.backend.common.enums.NotificationType;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private SpringTemplateEngine templateEngine;

    @InjectMocks
    private EmailNotificationService emailService;

    @Test
    void sendNotificationEmail_sendsEmailSuccessfully() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<html>content</html>");

        emailService.sendNotificationEmail("user@test.com", NotificationType.BOOKING_CREATED, Map.of());

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendNotificationEmail_retriesUpToThreeTimesOnFailure() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(any(String.class), any(Context.class))).thenReturn("<html>content</html>");
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP error"))
                .when(mailSender).send(any(MimeMessage.class));

        emailService.sendNotificationEmail("user@test.com", NotificationType.BOOKING_CONFIRMED, Map.of());

        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    void sendNotificationEmail_usesCorrectTemplateForBookingCreated() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/booking-request"), any(Context.class))).thenReturn("<html/>");

        emailService.sendNotificationEmail("user@test.com", NotificationType.BOOKING_CREATED, Map.of());

        verify(templateEngine).process(eq("email/booking-request"), any(Context.class));
    }

    @Test
    void sendNotificationEmail_usesCorrectTemplateForUserVerified() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/account-verified"), any(Context.class))).thenReturn("<html/>");

        emailService.sendNotificationEmail("user@test.com", NotificationType.USER_VERIFIED, Map.of());

        verify(templateEngine).process(eq("email/account-verified"), any(Context.class));
    }

    @Test
    void sendNotificationEmail_usesGenericTemplateForUnmappedType() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/notification-generic"), any(Context.class))).thenReturn("<html/>");

        emailService.sendNotificationEmail("user@test.com", NotificationType.ACCOUNT_DEACTIVATED, Map.of());

        verify(templateEngine).process(eq("email/notification-generic"), any(Context.class));
    }
}
