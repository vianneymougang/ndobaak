package com.ndobaak.backend.notification.service;

import com.ndobaak.backend.common.enums.NotificationType;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    public EmailNotificationService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendNotificationEmail(String to, NotificationType type, Map<String, Object> variables) {
        String templateName = resolveTemplateName(type);
        String subject = resolveSubject(type);

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Context context = new Context();
                context.setVariables(variables);
                String html = templateEngine.process(templateName, context);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(to);
                helper.setFrom("noreply@ndobaak.com", "Ndoba\u00e2k");
                helper.setSubject(subject);
                helper.setText(html, true);

                mailSender.send(message);
                return;
            } catch (Exception e) {
                log.error("Email send attempt {}/3 failed for type={} to={}: {}", attempt, type, to, e.getMessage());
            }
        }
    }

    private String resolveTemplateName(NotificationType type) {
        return switch (type) {
            case BOOKING_CREATED -> "email/booking-request";
            case BOOKING_CONFIRMED -> "email/booking-confirmed";
            case PAYMENT_RECEIVED -> "email/payment-confirmed";
            case USER_VERIFIED -> "email/account-verified";
            case USER_REJECTED -> "email/account-rejected";
            case SUBSCRIPTION_EXPIRING_SOON -> "email/subscription-expiring";
            case REVIEW_FLAGGED -> "email/review-moderated";
            default -> "email/notification-generic";
        };
    }

    private String resolveSubject(NotificationType type) {
        return switch (type) {
            case BOOKING_CREATED -> "Ndoba\u00e2k \u2014 Nouvelle demande de r\u00e9servation";
            case BOOKING_CONFIRMED -> "Ndoba\u00e2k \u2014 Votre r\u00e9servation est confirm\u00e9e";
            case BOOKING_CANCELLED -> "Ndoba\u00e2k \u2014 R\u00e9servation annul\u00e9e";
            case PAYMENT_RECEIVED -> "Ndoba\u00e2k \u2014 Paiement re\u00e7u";
            case PAYMENT_FAILED -> "Ndoba\u00e2k \u2014 \u00c9chec du paiement";
            case LISTING_APPROVED -> "Ndoba\u00e2k \u2014 Annonce approuv\u00e9e";
            case LISTING_REJECTED -> "Ndoba\u00e2k \u2014 Annonce refus\u00e9e";
            case USER_VERIFIED -> "Ndoba\u00e2k \u2014 Votre compte est v\u00e9rifi\u00e9";
            case USER_REJECTED -> "Ndoba\u00e2k \u2014 Votre compte a \u00e9t\u00e9 refus\u00e9";
            case SUBSCRIPTION_ACTIVATED -> "Ndoba\u00e2k \u2014 Abonnement activ\u00e9";
            case SUBSCRIPTION_EXPIRING_SOON -> "Ndoba\u00e2k \u2014 Abonnement bient\u00f4t expir\u00e9";
            case SUBSCRIPTION_EXPIRED -> "Ndoba\u00e2k \u2014 Abonnement expir\u00e9";
            case ACCOUNT_DEACTIVATED -> "Ndoba\u00e2k \u2014 Compte d\u00e9sactiv\u00e9";
            default -> "Ndoba\u00e2k \u2014 Nouvelle notification";
        };
    }
}
