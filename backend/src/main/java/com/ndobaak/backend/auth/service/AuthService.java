package com.ndobaak.backend.auth.service;

import com.ndobaak.backend.auth.dto.AuthResponse;
import com.ndobaak.backend.auth.dto.LoginRequest;
import com.ndobaak.backend.auth.dto.RefreshTokenRequest;
import com.ndobaak.backend.auth.dto.RegisterLandlordRequest;
import com.ndobaak.backend.auth.dto.RegisterStudentRequest;
import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.event.UserRegisteredEvent;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.DuplicateResourceException;
import com.ndobaak.backend.common.exception.ErrorCode;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.common.exception.UnauthorizedException;
import com.ndobaak.backend.common.security.JwtProperties;
import com.ndobaak.backend.common.security.JwtTokenProvider;
import com.ndobaak.backend.common.util.PhoneNormalizer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AuthResponse registerStudent(RegisterStudentRequest request) {
        validateUniqueEmailAndPhone(request.getEmail(), request.getPhone());

        Student student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(PhoneNormalizer.normalize(request.getPhone()))
                .password(passwordEncoder.encode(request.getPassword()))
                .matricule(request.getMatricule())
                .fieldOfStudy(request.getFieldOfStudy())
                .yearOfStudy(request.getYearOfStudy())
                .build();

        student = (Student) userRepository.save(student);
        eventPublisher.publishEvent(new UserRegisteredEvent(student.getId()));
        return generateTokens(student);
    }

    @Transactional
    public AuthResponse registerLandlord(RegisterLandlordRequest request) {
        validateUniqueEmailAndPhone(request.getEmail(), request.getPhone());

        Landlord landlord = Landlord.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(PhoneNormalizer.normalize(request.getPhone()))
                .password(passwordEncoder.encode(request.getPassword()))
                .idCardNumber(request.getIdCardNumber())
                .taxId(request.getTaxId())
                .build();

        landlord = (Landlord) userRepository.save(landlord);
        eventPublisher.publishEvent(new UserRegisteredEvent(landlord.getId()));
        return generateTokens(landlord);
    }

    public AuthResponse login(LoginRequest request) {
        User user;
        if (StringUtils.hasText(request.getEmail())) {
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS));
        } else if (StringUtils.hasText(request.getPhone())) {
            user = userRepository.findByPhone(PhoneNormalizer.normalize(request.getPhone()))
                    .orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS));
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Un email ou un numéro de téléphone est requis.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }

        return generateTokens(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED, "Session expirée. Veuillez vous reconnecter.");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED, "Session invalide. Veuillez vous reconnecter."));

        return generateTokens(user);
    }

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void validateUniqueEmailAndPhone(String email, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Cet email est déjà utilisé.");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("Ce numéro de téléphone est déjà utilisé.");
        }
    }

    private AuthResponse generateTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getExpirationMs() / 1000)
                .build();
    }
}
