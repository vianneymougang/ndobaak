package com.ndobaak.backend.rentalcycle.entity;

import com.ndobaak.backend.common.entity.BaseEntity;
import com.ndobaak.backend.common.enums.TenantDocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Getter
@Setter
@Entity
@Table(name = "tenant_document")
public class TenantDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private TenantApplication application;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "type", nullable = false, columnDefinition = "tenant_document_type")
    private TenantDocumentType type;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_url", nullable = false)
    private String storageUrl;
}
