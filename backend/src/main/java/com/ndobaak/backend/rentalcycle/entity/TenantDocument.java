package com.ndobaak.backend.rentalcycle.entity;

import com.ndobaak.backend.common.entity.BaseEntity;
import com.ndobaak.backend.common.enums.TenantDocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tenant_document")
public class TenantDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private TenantApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TenantDocumentType type;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_url", nullable = false)
    private String storageUrl;
}
