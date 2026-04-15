package com.ndobaak.backend.rentalcycle.entity;

import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.common.entity.BaseEntity;
import com.ndobaak.backend.common.enums.InspectionStatus;
import com.ndobaak.backend.common.enums.InspectionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "property_inspection")
@Getter
@Setter
public class PropertyInspection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false, length = 20)
    private InspectionType type;

    @Column(name = "conducted_at", nullable = false)
    private LocalDateTime conductedAt;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, length = 30)
    private InspectionStatus status;

    @Column(name = "landlord_signed", nullable = false)
    private boolean landlordSigned;

    @Column(name = "tenant_signed", nullable = false)
    private boolean tenantSigned;

    @Column(name = "landlord_signed_at")
    private LocalDateTime landlordSignedAt;

    @Column(name = "tenant_signed_at")
    private LocalDateTime tenantSignedAt;

    @Column(name = "observations")
    private String observations;

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InspectionItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL)
    private List<InspectionPhoto> photos = new ArrayList<>();
}
