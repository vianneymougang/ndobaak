package com.ndobaak.backend.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndobaak.backend.auth.dto.*;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.auth.service.CustomOAuth2UserService;
import com.ndobaak.backend.auth.service.PasswordResetService;
import com.ndobaak.backend.auth.service.UserProfileService;
import com.ndobaak.backend.common.enums.UserRole;
import com.ndobaak.backend.common.exception.DuplicateResourceException;
import com.ndobaak.backend.common.exception.ErrorCode;
import com.ndobaak.backend.common.exception.UnauthorizedException;
import com.ndobaak.backend.common.security.JwtAuthenticationFilter;
import com.ndobaak.backend.common.security.JwtProperties;
import com.ndobaak.backend.common.security.OAuth2AuthenticationFailureHandler;
import com.ndobaak.backend.common.security.OAuth2AuthenticationSuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserProfileService userProfileService;
    @MockitoBean
    private PasswordResetService passwordResetService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private JwtProperties jwtProperties;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private AuthResponse buildAuthResponse() {
        return AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600L)
                .build();
    }

    @Test
    void registerStudentShouldReturn201() throws Exception {
        RegisterStudentRequest request = new RegisterStudentRequest();
        request.setFirstName("Jean");
        request.setLastName("Mbarga");
        request.setEmail("jean@test.com");
        request.setPhone("690000001");
        request.setPassword("password123");

        when(authService.registerStudent(any())).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void registerStudentShouldReturn400WhenEmailMissing() throws Exception {
        RegisterStudentRequest request = new RegisterStudentRequest();
        request.setFirstName("Jean");
        request.setLastName("Mbarga");
        request.setPhone("690000001");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Les informations envoyées sont invalides."))
                .andExpect(jsonPath("$.retryable").value(false))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist());
    }

    @Test
    void registerStudentShouldReturn400WhenEmailInvalid() throws Exception {
        RegisterStudentRequest request = new RegisterStudentRequest();
        request.setFirstName("Jean");
        request.setLastName("Mbarga");
        request.setEmail("not-an-email");
        request.setPhone("690000001");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerStudentShouldReturn409WhenDuplicateEmail() throws Exception {
        RegisterStudentRequest request = new RegisterStudentRequest();
        request.setFirstName("Jean");
        request.setLastName("Mbarga");
        request.setEmail("exists@test.com");
        request.setPhone("690000001");
        request.setPassword("password123");

        when(authService.registerStudent(any())).thenThrow(new DuplicateResourceException("Email already registered"));

        mockMvc.perform(post("/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.retryable").value(false));
    }

    @Test
    void loginWithEmailShouldReturn200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("jean@test.com");
        request.setPassword("password123");

        when(authService.login(any())).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void loginWithInvalidCredentialsShouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("jean@test.com");
        request.setPassword("wrong");

        when(authService.login(any())).thenThrow(new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Identifiants invalides."))
                .andExpect(jsonPath("$.retryable").value(false));
    }

    @Test
    @WithMockUser
    void getCurrentUserShouldReturn200WhenAuthenticated() throws Exception {
        UserProfileResponse profile = UserProfileResponse.builder()
                .id(1L)
                .firstName("Jean")
                .lastName("Mbarga")
                .email("jean@test.com")
                .role(UserRole.STUDENT)
                .build();

        when(userProfileService.getCurrentUser()).thenReturn(profile);

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jean@test.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void forgotPasswordShouldReturn200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("jean@test.com");

        doNothing().when(passwordResetService).requestPasswordReset(any());

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
