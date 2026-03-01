package com.ndobaak.backend.payment.entity;

import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.common.entity.BaseEntity;
import com.ndobaak.backend.common.enums.ScheduleFrequency;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSchedule extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", unique = true)
    private Booking booking;

    @Column(name = "total_amount", precision = 12, scale = 0)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "schedule_frequency")
    private ScheduleFrequency frequency;

    @Column(name = "installment_count", nullable = false)
    private int numberOfInstallments;

    @OneToMany(mappedBy = "paymentSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Installment> installments = new ArrayList<>();
}
