package com.ndobaak.backend.rentalcycle.entity;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.common.entity.BaseEntity;
import com.ndobaak.backend.common.enums.DocumentStatus;
import com.ndobaak.backend.common.enums.LandlordDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "landlord_document")
@Getter
@Setter
public class LandlordDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private Landlord landlord;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "landlord_document_type")
    private LandlordDocumentType type;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_url", nullable = false)
    private String storageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "document_status")
    private DocumentStatus status;

    @Column(name = "validated_by_id")
    private Long validatedById;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "rejection_comment")
    private String rejectionComment;
}
