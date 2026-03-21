package com.ndobaak.backend.payment.mapper;

import com.ndobaak.backend.common.enums.InstallmentStatus;
import com.ndobaak.backend.payment.dto.InstallmentResponse;
import com.ndobaak.backend.payment.dto.PaymentResponse;
import com.ndobaak.backend.payment.dto.PaymentScheduleResponse;
import com.ndobaak.backend.payment.dto.ReceiptResponse;
import com.ndobaak.backend.payment.entity.Installment;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.entity.PaymentSchedule;
import com.ndobaak.backend.payment.gateway.PaymentProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class PaymentMapper {

    private final PaymentProperties paymentProperties;

    public PaymentMapper(PaymentProperties paymentProperties) {
        this.paymentProperties = paymentProperties;
    }

    public PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .amount(payment.getAmount())
                .commission(payment.getCommission())
                .totalCharged(payment.getAmount().add(payment.getCommission()))
                .status(payment.getStatus())
                .paymentType(payment.getPaymentType())
                .paymentMethod(payment.getPaymentMethod())
                .operator(payment.getMobileMoneyOperator())
                .transactionId(payment.getTransactionId())
                .providerName(payment.getProviderName())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public PaymentScheduleResponse toScheduleResponse(PaymentSchedule schedule) {
        List<InstallmentResponse> installmentResponses = schedule.getInstallments().stream()
                .map(this::toInstallmentResponse)
                .toList();

        long paidCount = schedule.getInstallments().stream()
                .filter(i -> i.getStatus() == InstallmentStatus.PAID)
                .count();

        BigDecimal paidAmount = schedule.getInstallments().stream()
                .filter(i -> i.getStatus() == InstallmentStatus.PAID)
                .map(Installment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PaymentScheduleResponse.builder()
                .id(schedule.getId())
                .bookingId(schedule.getBooking().getId())
                .totalAmount(schedule.getTotalAmount())
                .frequency(schedule.getFrequency())
                .numberOfInstallments(schedule.getNumberOfInstallments())
                .paidInstallments((int) paidCount)
                .paidAmount(paidAmount)
                .remainingAmount(schedule.getTotalAmount().subtract(paidAmount))
                .installments(installmentResponses)
                .createdAt(schedule.getCreatedAt())
                .build();
    }

    public InstallmentResponse toInstallmentResponse(Installment installment) {
        BigDecimal commission = installment.getAmount()
                .multiply(paymentProperties.getCommissionRate())
                .setScale(0, RoundingMode.CEILING);

        return InstallmentResponse.builder()
                .id(installment.getId())
                .installmentNumber(installment.getInstallmentNumber())
                .amount(installment.getAmount())
                .commission(commission)
                .dueDate(installment.getDueDate())
                .status(installment.getStatus())
                .paidAt(installment.getPaidAt())
                .build();
    }

    public ReceiptResponse toReceiptResponse(Payment payment) {
        String listingTitle = payment.getBooking().getListing().getTitle();
        String listingAddress = payment.getBooking().getListing().getLocation() != null
                ? payment.getBooking().getListing().getLocation().getAddress()
                : null;

        return ReceiptResponse.builder()
                .transactionId(payment.getTransactionId())
                .paymentDate(payment.getPaidAt())
                .payerName(payment.getPayerName())
                .payerPhone(payment.getPhoneNumber())
                .listingTitle(listingTitle)
                .listingAddress(listingAddress)
                .bookingStartDate(payment.getBooking().getStartDate())
                .bookingEndDate(payment.getBooking().getEndDate())
                .rentAmount(payment.getAmount())
                .commission(payment.getCommission())
                .totalCharged(payment.getAmount().add(payment.getCommission()))
                .paymentMethod(payment.getPaymentMethod())
                .operator(payment.getMobileMoneyOperator())
                .status(payment.getStatus())
                .paymentType(payment.getPaymentType())
                .build();
    }
}
