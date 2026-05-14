package com.ndobaak.backend.auth.service;

import com.ndobaak.backend.auth.dto.*;
import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.DuplicateResourceException;
import com.ndobaak.backend.common.exception.UnauthorizedException;
import com.ndobaak.backend.common.security.JwtProperties;
import com.ndobaak.backend.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerStudentShouldReturnTokens() {
        RegisterStudentRequest request = new RegisterStudentRequest();
        request.setFirstName("Jean");
        request.setLastName("Mbarga");
        request.setEmail("jean@test.com");
        request.setPhone("690000001");
        request.setPassword("password123");
        request.setMatricule("MAT001");

        when(userRepository.existsByEmail("jean@test.com")).thenReturn(false);
        when(userRepository.existsByPhone("690000001")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(jwtTokenProvider.generateAccessToken(eq(1L), eq("jean@test.com"), eq("STUDENT")))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(eq(1L), eq("jean@test.com")))
                .thenReturn("refresh-token");
        when(jwtProperties.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.registerStudent(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        verify(userRepository).save(any(Student.class));
    }

    @Test
    void registerStudentShouldFailOnDuplicateEmail() {
        RegisterStudentRequest request = new RegisterStudentRequest();
        request.setEmail("exists@test.com");
        request.setPhone("690000001");

        when(userRepository.existsByEmail("exists@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerStudent(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email est déjà utilisé");
    }

    @Test
    void registerLandlordShouldReturnTokens() {
        RegisterLandlordRequest request = new RegisterLandlordRequest();
        request.setFirstName("Paul");
        request.setLastName("Ndi");
        request.setEmail("paul@test.com");
        request.setPhone("690000002");
        request.setPassword("password123");
        request.setIdCardNumber("ID001");

        when(userRepository.existsByEmail("paul@test.com")).thenReturn(false);
        when(userRepository.existsByPhone("690000002")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(Landlord.class))).thenAnswer(invocation -> {
            Landlord l = invocation.getArgument(0);
            l.setId(2L);
            return l;
        });
        when(jwtTokenProvider.generateAccessToken(eq(2L), eq("paul@test.com"), eq("LANDLORD")))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(eq(2L), eq("paul@test.com")))
                .thenReturn("refresh-token");
        when(jwtProperties.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.registerLandlord(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(userRepository).save(any(Landlord.class));
    }

    @Test
    void loginWithEmailShouldReturnTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("jean@test.com");
        request.setPassword("password123");

        Student student = Student.builder()
                .firstName("Jean").lastName("Mbarga")
                .email("jean@test.com").phone("690000001")
                .password("$2a$10$hashed").build();
        student.setId(1L);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("password123", "$2a$10$hashed")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(eq(1L), eq("jean@test.com"), eq("STUDENT")))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(eq(1L), eq("jean@test.com")))
                .thenReturn("refresh-token");
        when(jwtProperties.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void loginWithWrongPasswordShouldThrow() {
        LoginRequest request = new LoginRequest();
        request.setEmail("jean@test.com");
        request.setPassword("wrong");

        Student student = Student.builder()
                .email("jean@test.com").password("$2a$10$hashed").build();

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Identifiants invalides");
    }

    @Test
    void loginWithPhoneShouldReturnTokens() {
        LoginRequest request = new LoginRequest();
        request.setPhone("690000001");
        request.setPassword("password123");

        Student student = Student.builder()
                .email("jean@test.com").phone("+237690000001")
                .password("$2a$10$hashed").build();
        student.setId(1L);

        when(userRepository.findByPhone("+237690000001")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("password123", "$2a$10$hashed")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(eq(1L), eq("jean@test.com"), eq("STUDENT")))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(eq(1L), eq("jean@test.com")))
                .thenReturn("refresh-token");
        when(jwtProperties.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void loginWithoutEmailOrPhoneShouldThrow() {
        LoginRequest request = new LoginRequest();
        request.setPassword("password123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email ou un numéro de téléphone");
    }

    @Test
    void refreshTokenShouldReturnNewTokens() {
        when(jwtTokenProvider.validateToken("old-refresh")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("old-refresh")).thenReturn("jean@test.com");

        Student student = Student.builder()
                .email("jean@test.com").build();
        student.setId(1L);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(student));
        when(jwtTokenProvider.generateAccessToken(eq(1L), eq("jean@test.com"), eq("STUDENT")))
                .thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(eq(1L), eq("jean@test.com")))
                .thenReturn("new-refresh");
        when(jwtProperties.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.refreshToken("old-refresh");

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refreshTokenWithInvalidTokenShouldThrow() {
        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("bad-token"))
                .isInstanceOf(UnauthorizedException.class);
    }


    private void setupSecurityContext(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        when(authentication.isAuthenticated()).thenReturn(true);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
