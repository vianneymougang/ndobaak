package com.ndobaak.backend.auth.service;

import com.ndobaak.backend.auth.dto.ChangePasswordRequest;
import com.ndobaak.backend.auth.dto.UpdateProfileRequest;
import com.ndobaak.backend.auth.dto.UserProfileResponse;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.mapper.UserMapper;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private AuthService authService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void getCurrentUserShouldReturnProfile() {
        Student student = Student.builder()
                .email("jean@test.com").firstName("Jean").build();
        student.setId(1L);

        UserProfileResponse profileResponse = UserProfileResponse.builder()
                .id(1L).firstName("Jean").email("jean@test.com").build();

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(userMapper.toProfileResponse(student)).thenReturn(profileResponse);

        UserProfileResponse result = userProfileService.getCurrentUser();

        assertThat(result.getFirstName()).isEqualTo("Jean");
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void changePasswordShouldUpdateWhenOldPasswordCorrect() {
        Student student = Student.builder()
                .email("jean@test.com").password("$2a$10$old").build();
        student.setId(1L);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(passwordEncoder.matches("oldPassword", "$2a$10$old")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("$2a$10$new");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword");

        userProfileService.changePassword(request);

        verify(userRepository).save(student);
        assertThat(student.getPassword()).isEqualTo("$2a$10$new");
    }

    @Test
    void changePasswordShouldFailWhenOldPasswordWrong() {
        Student student = Student.builder()
                .email("jean@test.com").password("$2a$10$old").build();

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(passwordEncoder.matches("wrongOld", "$2a$10$old")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongOld");
        request.setNewPassword("newPassword");

        assertThatThrownBy(() -> userProfileService.changePassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mot de passe actuel est incorrect");
    }

    @Test
    void updateProfileShouldUpdateFirstName() {
        Student student = Student.builder()
                .email("jean@test.com").firstName("Jean").lastName("Mbarga").build();
        student.setId(1L);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Pierre");

        UserProfileResponse expected = UserProfileResponse.builder()
                .id(1L).firstName("Pierre").build();

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(userRepository.save(any())).thenReturn(student);
        when(userMapper.toProfileResponse(student)).thenReturn(expected);

        UserProfileResponse result = userProfileService.updateProfile(request);

        assertThat(result.getFirstName()).isEqualTo("Pierre");
        assertThat(student.getFirstName()).isEqualTo("Pierre");
    }
}
