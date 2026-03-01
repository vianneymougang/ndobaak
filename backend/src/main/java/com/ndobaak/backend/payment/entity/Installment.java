package com.ndobaak.backend.payment.entity;

import com.ndobaak.backend.common.entity.BaseEntity;
import com.ndobaak.backend.common.enums.InstallmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "installment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_schedule_id", nullable = false)
    private PaymentSchedule paymentSchedule;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "installment_status")
    private InstallmentStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "actual_payment_date")
    private LocalDateTime paidAt;
}
