package com.ndobaak.backend.auth.controller;

import com.ndobaak.backend.auth.dto.*;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.auth.service.PasswordResetService;
import com.ndobaak.backend.common.security.CookieUtils;
import com.ndobaak.backend.common.security.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, PasswordResetService passwordResetService,
                          JwtProperties jwtProperties) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register/student")
    public ResponseEntity<AuthResponse> registerStudent(@Valid @RequestBody RegisterStudentRequest request,
                                                        HttpServletResponse response) {
        AuthResponse authResponse = authService.registerStudent(request);
        setAuthCookies(response, authResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/register/landlord")
    public ResponseEntity<AuthResponse> registerLandlord(@Valid @RequestBody RegisterLandlordRequest request,
                                                         HttpServletResponse response) {
        AuthResponse authResponse = authService.registerLandlord(request);
        setAuthCookies(response, authResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        setAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                HttpServletResponse response) {
        AuthResponse authResponse = authService.refreshToken(request.getRefreshToken());
        setAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        CookieUtils.clearAuthCookies(response, jwtProperties.isSecureCookies());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    private void setAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
        boolean secure = jwtProperties.isSecureCookies();
        CookieUtils.addAccessCookie(response, authResponse.getAccessToken(),
                jwtProperties.getExpirationMs() / 1000, secure);
        CookieUtils.addRefreshCookie(response, authResponse.getRefreshToken(),
                jwtProperties.getRefreshExpirationMs() / 1000, secure);
    }
}
