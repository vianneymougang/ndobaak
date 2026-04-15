package com.ndobaak.backend.rentalcycle.entity;

import com.ndobaak.backend.common.entity.BaseEntity;
import com.ndobaak.backend.common.enums.ElementCondition;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "inspection_item")
@Getter
@Setter
public class InspectionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private PropertyInspection inspection;

    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @Column(name = "element", nullable = false, length = 100)
    private String element;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "condition", nullable = false, length = 20)
    private ElementCondition condition;

    @Column(name = "observations")
    private String observations;
}
