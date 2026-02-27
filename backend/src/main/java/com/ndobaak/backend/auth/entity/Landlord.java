package com.ndobaak.backend.auth.entity;

import com.ndobaak.backend.common.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("LANDLORD")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Landlord extends User {

    @Column(name = "id_card_number", length = 50)
    private String idCardNumber;

    @Column(name = "tax_id", length = 200)
    private String taxId;

    @Override
    public UserRole getRole() {
        return UserRole.LANDLORD;
    }
}
