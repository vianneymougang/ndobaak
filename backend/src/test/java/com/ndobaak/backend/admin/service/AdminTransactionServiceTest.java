package com.ndobaak.backend.admin.service;

import com.ndobaak.backend.admin.dto.AdminTransactionResponse;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTransactionServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private AdminTransactionService adminTransactionService;

    private Payment buildPayment(Long id, BigDecimal amount, PaymentStatus status, PaymentType type) {
        Payment payment = Payment.builder()
                .amount(amount)
                .status(status)
                .paymentType(type)
                .payerName("Test Payer")
                .phoneNumber("699000001")
                .build();
        payment.setId(id);
        return payment;
    }

    @SuppressWarnings("unchecked")
    @Test
    void listTransactionsShouldReturnPagedResultsWithNoFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        Payment p = buildPayment(1L, BigDecimal.valueOf(10_000), PaymentStatus.COMPLETED, PaymentType.INTEGRAL);
        when(paymentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<AdminTransactionResponse> result = adminTransactionService.listTransactions(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    @SuppressWarnings("unchecked")
    @Test
    void listTransactionsShouldApplyStatusFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Payment p = buildPayment(2L, BigDecimal.valueOf(20_000), PaymentStatus.FAILED, PaymentType.INTEGRAL);
        when(paymentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<AdminTransactionResponse> result = adminTransactionService.listTransactions("FAILED", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @SuppressWarnings("unchecked")
    @Test
    void listTransactionsShouldApplyTypeFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Payment p = buildPayment(3L, BigDecimal.valueOf(5_000), PaymentStatus.COMPLETED, PaymentType.SUBSCRIPTION);
        when(paymentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<AdminTransactionResponse> result = adminTransactionService.listTransactions(null, "SUBSCRIPTION", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPaymentType()).isEqualTo(PaymentType.SUBSCRIPTION);
    }

    @SuppressWarnings("unchecked")
    @Test
    void listTransactionsShouldIgnoreInvalidFilterValues() {
        Pageable pageable = PageRequest.of(0, 20);
        when(paymentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<AdminTransactionResponse> result = adminTransactionService.listTransactions("INVALID_STATUS", "UNKNOWN_TYPE", pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getTransactionDetailShouldReturnResponseWhenFound() {
        Payment p = buildPayment(10L, BigDecimal.valueOf(50_000), PaymentStatus.COMPLETED, PaymentType.INTEGRAL);
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(p));

        AdminTransactionResponse result = adminTransactionService.getTransactionDetail(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void getTransactionDetailShouldThrowWhenNotFound() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminTransactionService.getTransactionDetail(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void detectAnomaliesShouldFlagHighAmountPayments() {
        Payment highAmount = buildPayment(1L, BigDecimal.valueOf(600_000), PaymentStatus.COMPLETED, PaymentType.INTEGRAL);
        Payment normal = buildPayment(2L, BigDecimal.valueOf(15_000), PaymentStatus.COMPLETED, PaymentType.INTEGRAL);
        when(paymentRepository.findAll()).thenReturn(List.of(highAmount, normal));

        List<AdminTransactionResponse> anomalies = adminTransactionService.detectAnomalies();

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.get(0).getId()).isEqualTo(1L);
        assertThat(anomalies.get(0).isAnomaly()).isTrue();
        assertThat(anomalies.get(0).getAnomalyReason()).contains("FCFA");
    }

    @Test
    void detectAnomaliesShouldFlagFailedSubscriptionPayments() {
        Payment failedSub = buildPayment(5L, BigDecimal.valueOf(3_000), PaymentStatus.FAILED, PaymentType.SUBSCRIPTION);
        Payment failedIntegral = buildPayment(6L, BigDecimal.valueOf(3_000), PaymentStatus.FAILED, PaymentType.INTEGRAL);
        when(paymentRepository.findAll()).thenReturn(List.of(failedSub, failedIntegral));

        List<AdminTransactionResponse> anomalies = adminTransactionService.detectAnomalies();

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.get(0).getId()).isEqualTo(5L);
        assertThat(anomalies.get(0).getAnomalyReason()).contains("abonnement");
    }
}
