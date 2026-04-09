package com.ndobaak.backend.rentalcycle.entity;

import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.common.entity.BaseEntity;
import com.ndobaak.backend.common.enums.LeaseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "lease_contract")
public class LeaseContract extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "monthly_rent", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "security_deposit", nullable = false, precision = 19, scale = 2)
    private BigDecimal securityDeposit;

    @Column(name = "agency_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal agencyFee;

    @Column(name = "special_clauses")
    private String specialClauses;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "lease_status")
    private LeaseStatus status;

    @Column(name = "landlord_signed_at")
    private LocalDateTime landlordSignedAt;

    @Column(name = "tenant_signed_at")
    private LocalDateTime tenantSignedAt;

    @Column(name = "tax_registration_date")
    private LocalDate taxRegistrationDate;

    @Column(name = "tax_reference", length = 100)
    private String taxReference;

    @Column(name = "generated_by_id", nullable = false)
    private Long generatedById;
}
