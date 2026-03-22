package com.ndobaak.backend.auth.mapper;

import com.ndobaak.backend.auth.dto.UserProfileResponse;
import com.ndobaak.backend.auth.entity.Admin;
import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .role(user.getRole())
                .registrationDate(user.getRegistrationDate());

        if (user instanceof Student student) {
            builder.matricule(student.getMatricule())
                    .fieldOfStudy(student.getFieldOfStudy())
                    .yearOfStudy(student.getYearOfStudy());
        } else if (user instanceof Landlord landlord) {
            builder.verified(landlord.getIdCardNumber() != null);
        }

        return builder.build();
    }
}
