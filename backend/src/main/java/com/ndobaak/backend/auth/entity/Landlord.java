package com.ndobaak.backend.auth.entity;

import com.ndobaak.backend.common.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "^\\+237[26][0-9]{8}$")
    @Column(name = "mobile_money_phone", length = 20)
    private String mobileMoneyPhone;

    @Column(name = "notch_pay_account_id", length = 100)
    private String notchPayAccountId;

    @Override
    public UserRole getRole() {
        return UserRole.LANDLORD;
    }
}
