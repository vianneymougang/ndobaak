package com.ndobaak.backend.rentalcycle.entity;

import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "key_handover")
@Getter
@Setter
public class KeyHandover extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "move_in_inspection_id", nullable = false)
    private PropertyInspection moveInInspection;

    @Column(name = "handover_date", nullable = false)
    private LocalDateTime handoverDate;

    @Column(name = "landlord_confirmed", nullable = false)
    private boolean landlordConfirmed;

    @Column(name = "tenant_confirmed", nullable = false)
    private boolean tenantConfirmed;

    @Column(name = "landlord_confirmed_at")
    private LocalDateTime landlordConfirmedAt;

    @Column(name = "tenant_confirmed_at")
    private LocalDateTime tenantConfirmedAt;

    @Column(name = "transfer_initiated", nullable = false)
    private boolean transferInitiated;

    @Column(name = "transfer_date")
    private LocalDateTime transferDate;

    @Column(name = "observations")
    private String observations;
}
