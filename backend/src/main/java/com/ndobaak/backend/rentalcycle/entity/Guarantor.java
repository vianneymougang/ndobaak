package com.ndobaak.backend.rentalcycle.entity;

import com.ndobaak.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "guarantor")
public class Guarantor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private TenantApplication application;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "id_card_number", nullable = false, length = 50)
    private String idCardNumber;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "relationship_to_tenant", nullable = false, length = 100)
    private String relationshipToTenant;
}
