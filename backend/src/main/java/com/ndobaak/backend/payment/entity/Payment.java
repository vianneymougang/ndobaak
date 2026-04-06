package com.ndobaak.backend.payment.entity;

import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.common.entity.BaseEntity;
import com.ndobaak.backend.common.enums.PaymentChannel;
import com.ndobaak.backend.common.enums.PaymentMethod;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Column(precision = 12, scale = 0)
    private BigDecimal commission;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "channel", columnDefinition = "payment_channel", length = 20)
    private PaymentChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 50)
    private PaymentType paymentType;

    @Column(name = "transaction_reference", unique = true, length = 100)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_status")
    private PaymentStatus status;

    @Column(name = "mobile_money_number", length = 20)
    private String phoneNumber;

    @Column(name = "payer_name", length = 200)
    private String payerName;

    @Column(name = "payment_date")
    private LocalDateTime paidAt;

    @Column(name = "provider_name", length = 50)
    private String providerName;

    @Column(name = "external_reference", length = 100)
    private String providerReference;
}
