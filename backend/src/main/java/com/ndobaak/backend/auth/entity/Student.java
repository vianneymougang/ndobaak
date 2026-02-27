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
@DiscriminatorValue("STUDENT")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Student extends User {

    @Column(length = 50)
    private String matricule;

    @Column(name = "field_of_study", length = 100)
    private String fieldOfStudy;

    @Column(name = "year_of_study")
    private Integer yearOfStudy;

    @Override
    public UserRole getRole() {
        return UserRole.STUDENT;
    }
}
