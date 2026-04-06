package com.ndobaak.backend.admin.service;

import com.ndobaak.backend.admin.dto.AdminTransactionResponse;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminTransactionService {

    private static final BigDecimal ANOMALY_HIGH_AMOUNT = BigDecimal.valueOf(500_000);

    private final PaymentRepository paymentRepository;

    public AdminTransactionService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> listTransactions(String statusFilter,
                                                            String typeFilter,
                                                            Pageable pageable) {
        Specification<Payment> spec = Specification.where(null);

        PaymentStatus status = parseEnum(statusFilter, PaymentStatus.class);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        PaymentType type = parseEnum(typeFilter, PaymentType.class);
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("paymentType"), type));
        }

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return paymentRepository.findAll(spec, sorted)
                .map(p -> toResponse(p, detectAnomaly(p)));
    }

    @Transactional(readOnly = true)
    public AdminTransactionResponse getTransactionDetail(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        return toResponse(payment, detectAnomaly(payment));
    }

    @Transactional(readOnly = true)
    public List<AdminTransactionResponse> detectAnomalies() {
        return paymentRepository.findAll().stream()
                .filter(this::detectAnomaly)
                .map(p -> toResponse(p, true))
                .toList();
    }

    private boolean detectAnomaly(Payment payment) {
        if (payment.getAmount() != null && payment.getAmount().compareTo(ANOMALY_HIGH_AMOUNT) > 0) {
            return true;
        }
        if (payment.getStatus() == PaymentStatus.FAILED && payment.getPaymentType() == PaymentType.SUBSCRIPTION) {
            return true;
        }
        return false;
    }

    private String anomalyReason(Payment payment) {
        if (payment.getAmount() != null && payment.getAmount().compareTo(ANOMALY_HIGH_AMOUNT) > 0) {
            return "Montant anormalement élevé (" + payment.getAmount() + " FCFA)";
        }
        if (payment.getStatus() == PaymentStatus.FAILED && payment.getPaymentType() == PaymentType.SUBSCRIPTION) {
            return "Paiement d'abonnement échoué";
        }
        return null;
    }

    private AdminTransactionResponse toResponse(Payment payment, boolean anomaly) {
        return AdminTransactionResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .paymentType(payment.getPaymentType())
                .status(payment.getStatus())
                .payerName(payment.getPayerName())
                .phoneNumber(payment.getPhoneNumber())
                .operator(payment.getChannel())
                .providerName(payment.getProviderName())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .bookingId(payment.getBooking() != null ? payment.getBooking().getId() : null)
                .anomaly(anomaly)
                .anomalyReason(anomaly ? anomalyReason(payment) : null)
                .build();
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumClass) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
