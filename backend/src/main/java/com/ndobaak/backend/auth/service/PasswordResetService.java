package com.ndobaak.backend.auth.service;

import com.ndobaak.backend.auth.dto.ForgotPasswordRequest;
import com.ndobaak.backend.auth.dto.ResetPasswordRequest;
import com.ndobaak.backend.auth.entity.PasswordResetToken;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.repository.PasswordResetTokenRepository;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ErrorCode;
import com.ndobaak.backend.common.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 30;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        if (user.getPassword() == null) {
            return;
        }

        tokenRepository.deleteByUserAndUsedAtIsNull(user);

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                .build();

        tokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/auth/reset-password?token=" + rawToken;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetLink);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = sha256(request.getToken());

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("Lien de réinitialisation invalide ou expiré."));

        if (resetToken.isExpired()) {
            throw new BusinessException(ErrorCode.RESET_TOKEN_EXPIRED);
        }

        if (resetToken.isUsed()) {
            throw new BusinessException(ErrorCode.RESET_TOKEN_USED);
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.markUsed();
        tokenRepository.save(resetToken);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
