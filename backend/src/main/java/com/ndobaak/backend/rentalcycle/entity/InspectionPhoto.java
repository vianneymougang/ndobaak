package com.ndobaak.backend.rentalcycle.entity;

import com.ndobaak.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inspection_photo")
@Getter
@Setter
public class InspectionPhoto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private PropertyInspection inspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private InspectionItem item;

    @Column(name = "storage_url", nullable = false)
    private String storageUrl;

    @Column(name = "caption")
    private String caption;
}
