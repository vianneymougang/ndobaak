package com.ndobaak.backend.payment.service;

import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.notification.service.NotificationDispatcher;
import com.ndobaak.backend.payment.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PaymentNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationService.class);

    private final NotificationDispatcher notificationDispatcher;
    private final BookingRepository bookingRepository;

    public PaymentNotificationService(NotificationDispatcher notificationDispatcher,
                                      BookingRepository bookingRepository) {
        this.notificationDispatcher = notificationDispatcher;
        this.bookingRepository = bookingRepository;
    }

    void sendPaymentCompletionNotifications(Payment payment, Booking booking) {
        booking.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking);

        String totalFormatted = formatAmount(booking.getTotalAmount());
        String listingTitle = booking.getListing().getTitle();

        notificationDispatcher.dispatch(
                booking.getStudent().getId(),
                NotificationType.PAYMENT_RECEIVED,
                "Paiement reçu",
                "Votre paiement de " + totalFormatted + " FCFA pour \""
                        + listingTitle + "\" a été confirmé.",
                payment.getId(),
                "PAYMENT",
                Map.of(
                        "payerName", payment.getPayerName(),
                        "amount", totalFormatted + " FCFA",
                        "listingTitle", listingTitle,
                        "receiptUrl", ""
                ),
                Map.of(
                        "amount", totalFormatted,
                        "reference", payment.getTransactionId()
                )
        );

        notificationDispatcher.dispatch(
                booking.getListing().getLandlord().getId(),
                NotificationType.PAYMENT_RECEIVED,
                "Paiement reçu",
                "Un paiement de " + totalFormatted + " FCFA a été reçu pour \""
                        + listingTitle + "\".",
                payment.getId(),
                "PAYMENT",
                Map.of(
                        "payerName", payment.getPayerName(),
                        "amount", totalFormatted + " FCFA",
                        "listingTitle", listingTitle,
                        "receiptUrl", ""
                ),
                Map.of(
                        "amount", totalFormatted,
                        "reference", payment.getTransactionId()
                )
        );

        log.info("Payment fully completed: booking={}, total={} XAF",
                booking.getId(), booking.getTotalAmount());
    }

    void sendPaymentFailedNotification(Payment payment, Booking booking) {
        notificationDispatcher.dispatch(
                booking.getStudent().getId(),
                NotificationType.PAYMENT_FAILED,
                "Échec du paiement",
                "Votre paiement pour \"" + booking.getListing().getTitle()
                        + "\" a échoué. Veuillez réessayer.",
                payment.getId(),
                "PAYMENT",
                Map.of("listingTitle", booking.getListing().getTitle()),
                Map.of()
        );

        log.warn("Payment failed: reference={}", payment.getTransactionId());
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%,.0f", amount);
    }
}
