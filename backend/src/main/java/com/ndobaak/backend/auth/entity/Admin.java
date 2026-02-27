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
@DiscriminatorValue("ADMIN")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Admin extends User {

    @Column(name = "role", length = 50)
    private String adminRole;

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }
}
